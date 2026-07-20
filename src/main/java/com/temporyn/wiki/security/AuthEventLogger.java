package com.temporyn.wiki.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Writes structured authentication events to the dedicated {@code AUTH_EVENT} logger,
 * which logback routes to a per-day rolling file. Sensitive material (password, TOTP
 * code/secret, session cookies, tokens) is never logged.
 */
@Component
public class AuthEventLogger {

    private static final Logger LOG = LoggerFactory.getLogger("AUTH_EVENT");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter UTC_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter KST_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'KST'").withZone(KST);
    private static final String DEVICE_COOKIE = "device_id";

    public void logSuccess(HttpServletRequest request, String account, String method) {
        log(request, account, "SUCCESS", "-", method, null, "-", "-");
    }

    public void logFailure(HttpServletRequest request, String account, String failureStage, String reason) {
        log(request, account, "FAILURE", failureStage, "-", reason, "-", "-");
    }

    /** Record a failed login together with any rate-limit and auto-block outcome. */
    public void logFailure(HttpServletRequest request, String account, String failureStage, String reason,
            String rateLimit, String autoBlock) {
        log(request, account, "FAILURE", failureStage, "-", reason, rateLimit, autoBlock);
    }

    /** Record a request rejected up front by the rate limiter (before credential checks). */
    public void logRateLimited(HttpServletRequest request, String account, String reason, long blockedUntilEpochMs) {
        String autoBlock = blockedUntilEpochMs > 0 ? "until " + KST_FMT.format(Instant.ofEpochMilli(blockedUntilEpochMs)) : "-";
        log(request, account, "BLOCKED", "RATE_LIMIT", "-", reason, reason, autoBlock);
    }

    private void log(HttpServletRequest request, String account, String result,
            String failureStage, String method, String reason, String rateLimit, String autoBlock) {
        Instant now = Instant.now();
        StringBuilder sb = new StringBuilder();
        append(sb, "eventId", UUID.randomUUID().toString());
        append(sb, "occurredAtUtc", UTC_FMT.format(now));
        append(sb, "displayTimeKst", KST_FMT.format(now));
        append(sb, "account", account);
        append(sb, "result", result);
        append(sb, "failureStage", failureStage);
        if (reason != null) {
            append(sb, "reason", reason);
        }
        append(sb, "authMethod", method);
        append(sb, "sourceIp", normalizeIp(request != null ? request.getRemoteAddr() : "-"));
        append(sb, "clientIp", clientIp(request));
        append(sb, "userAgent", request != null ? request.getHeader("User-Agent") : "-");
        append(sb, "path", request != null ? request.getRequestURI() : "-");
        append(sb, "deviceIdHash", deviceIdHash(request));
        // These require services not yet implemented; kept for a stable schema.
        append(sb, "newIp", "n/a");
        append(sb, "newDevice", "n/a");
        append(sb, "rateLimit", rateLimit);
        append(sb, "autoBlock", autoBlock);
        append(sb, "mailStatus", "SMTP_NOT_CONFIGURED");
        LOG.info(sb.toString().trim());
    }

    /** Client IP forwarded by a trusted proxy, if any (first hop of X-Forwarded-For). */
    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "-";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return "-";
        }
        int comma = forwarded.indexOf(',');
        return normalizeIp((comma < 0 ? forwarded : forwarded.substring(0, comma)).trim());
    }

    /** Collapse an IPv4-mapped IPv6 address (::ffff:203.0.113.25) to plain IPv4. */
    private String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "-";
        }
        String value = ip.trim();
        int prefix = value.lastIndexOf(':');
        if (prefix >= 0 && value.regionMatches(true, 0, "::ffff:", 0, 7)) {
            String tail = value.substring(prefix + 1);
            if (tail.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
                return tail;
            }
        }
        return value;
    }

    /** SHA-256 hash of the known-device cookie value, or "-" when absent. */
    private String deviceIdHash(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return "-";
        }
        for (Cookie cookie : request.getCookies()) {
            if (DEVICE_COOKIE.equals(cookie.getName()) && cookie.getValue() != null) {
                return sha256(cookie.getValue());
            }
        }
        return "-";
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            return "-";
        }
    }

    /** Append a key=value pair, sanitizing values to keep each event on one line. */
    private void append(StringBuilder sb, String key, String value) {
        String safe = value == null ? "-" : value.replaceAll("[\\r\\n\\t]", " ").trim();
        if (safe.isEmpty()) {
            safe = "-";
        }
        sb.append(key).append("=\"").append(safe.replace("\"", "'")).append("\" ");
    }
}
