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
        log(request, account, "SUCCESS", "-", method, null);
    }

    public void logFailure(HttpServletRequest request, String account, String failureStage, String reason) {
        log(request, account, "FAILURE", failureStage, "-", reason);
    }

    private void log(HttpServletRequest request, String account, String result,
            String failureStage, String method, String reason) {
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
        append(sb, "sourceIp", request != null ? request.getRemoteAddr() : "-");
        append(sb, "clientIp", clientIp(request));
        append(sb, "userAgent", request != null ? request.getHeader("User-Agent") : "-");
        append(sb, "path", request != null ? request.getRequestURI() : "-");
        append(sb, "deviceIdHash", deviceIdHash(request));
        // The following require state/services not yet implemented; kept for a stable schema.
        append(sb, "newIp", "n/a");
        append(sb, "newDevice", "n/a");
        append(sb, "rateLimit", "n/a");
        append(sb, "autoBlock", "n/a");
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
        return (comma < 0 ? forwarded : forwarded.substring(0, comma)).trim();
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
