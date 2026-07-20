package com.temporyn.wiki.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * In-memory brute-force protection for the single-instance login flow.
 *
 * <p>Tracks recent login requests and failures per IP, per account, and per IP+account
 * combination, applies progressive delays, and computes temporary blocks. It also tracks
 * the post-password TOTP transaction (max attempts and expiry). All windows are sliding
 * and state is pruned lazily; nothing is persisted.
 */
@Service
public class LoginAttemptService {

    private static final long MINUTE = 60_000L;

    // IP request rate: at most 5 login requests per minute.
    private static final int IP_REQUESTS_PER_MINUTE = 5;

    // IP failures: 20 within 15 minutes -> block for 30 minutes.
    private static final int IP_FAIL_LIMIT = 20;
    private static final long IP_FAIL_WINDOW = 15 * MINUTE;
    private static final long IP_BLOCK_DURATION = 30 * MINUTE;

    // Account failures: progressive delay from 5 within 15 minutes; block 15 minutes at 10 within 1 hour.
    private static final int ACCOUNT_DELAY_THRESHOLD = 5;
    private static final long ACCOUNT_DELAY_WINDOW = 15 * MINUTE;
    private static final int ACCOUNT_BLOCK_LIMIT = 10;
    private static final long ACCOUNT_BLOCK_WINDOW = 60 * MINUTE;
    private static final long ACCOUNT_BLOCK_DURATION = 15 * MINUTE;

    // IP + account: 5 within 10 minutes -> block 15 minutes.
    private static final int COMBO_LIMIT = 5;
    private static final long COMBO_WINDOW = 10 * MINUTE;
    private static final long COMBO_BLOCK_DURATION = 15 * MINUTE;

    // Progressive delay steps (2s, 4s, 8s) once the account delay threshold is reached.
    private static final long MAX_DELAY_MILLIS = 8_000L;

    // TOTP transaction: max 5 attempts, expires 5 minutes after the last attempt.
    private static final int TOTP_MAX_ATTEMPTS = 5;
    private static final long TOTP_TRANSACTION_TTL = 5 * MINUTE;

    private final Deque<Long> ipRequests(String ip) {
        return ipRequestLog.computeIfAbsent(ip, k -> new ArrayDeque<>());
    }

    private final ConcurrentHashMap<String, Deque<Long>> ipRequestLog = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Long>> ipFailures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Long>> accountFailures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Long>> comboFailures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> blockedUntil = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TotpTransaction> totpTransactions = new ConcurrentHashMap<>();

    /** Result of a pre-authentication check for a login request. */
    public record Decision(boolean allowed, String reason, long blockedUntilEpochMs) {
        static Decision allow() {
            return new Decision(true, null, 0);
        }

        static Decision deny(String reason, long until) {
            return new Decision(false, reason, until);
        }
    }

    /**
     * Check whether a login request may proceed. Records the request against the
     * per-minute IP quota. Does not record failures.
     */
    public Decision checkRequest(String ip, String account) {
        long now = System.currentTimeMillis();

        long blocked = activeBlock(ip, account, now);
        if (blocked > 0) {
            return Decision.deny("TEMP_BLOCK", blocked);
        }

        Deque<Long> requests = ipRequests(ip);
        synchronized (requests) {
            prune(requests, now, MINUTE);
            if (requests.size() >= IP_REQUESTS_PER_MINUTE) {
                return Decision.deny("IP_RATE", now + MINUTE);
            }
            requests.addLast(now);
        }
        return Decision.allow();
    }

    /** Progressive delay (ms) to apply before processing, based on recent account failures. */
    public long delayMillis(String account) {
        if (account == null || account.isBlank()) {
            return 0;
        }
        long now = System.currentTimeMillis();
        int recent = countWithin(accountFailures.get(account), now, ACCOUNT_DELAY_WINDOW);
        if (recent < ACCOUNT_DELAY_THRESHOLD - 2) {
            return 0;
        }
        long delay = 2_000L << Math.max(0, recent - (ACCOUNT_DELAY_THRESHOLD - 2) - 1);
        return Math.min(delay, MAX_DELAY_MILLIS);
    }

    /** Record a failed login and update blocks. Returns a description of any block applied. */
    public String recordFailure(String ip, String account) {
        long now = System.currentTimeMillis();
        String applied = "-";

        int ipCount = record(ipFailures, ip, now, IP_FAIL_WINDOW);
        if (ipCount >= IP_FAIL_LIMIT) {
            block("ip:" + ip, now + IP_BLOCK_DURATION);
            applied = "IP_BLOCK_30M";
        }

        if (account != null && !account.isBlank()) {
            int accCount = record(accountFailures, account, now, ACCOUNT_BLOCK_WINDOW);
            if (accCount >= ACCOUNT_BLOCK_LIMIT) {
                block("acc:" + account, now + ACCOUNT_BLOCK_DURATION);
                applied = "ACCOUNT_BLOCK_15M";
            }
            int comboCount = record(comboFailures, ip + "|" + account, now, COMBO_WINDOW);
            if (comboCount >= COMBO_LIMIT) {
                block("combo:" + ip + "|" + account, now + COMBO_BLOCK_DURATION);
                applied = "COMBO_BLOCK_15M";
            }
        }
        return applied;
    }

    /** Clear counters and the TOTP transaction after a successful login. */
    public void recordSuccess(String ip, String account) {
        ipFailures.remove(ip);
        if (account != null) {
            accountFailures.remove(account);
            comboFailures.remove(ip + "|" + account);
            totpTransactions.remove(ip + "|" + account);
        }
    }

    /** Whether the current TOTP transaction has used up its allowed attempts. */
    public boolean isTotpTransactionExhausted(String ip, String account) {
        TotpTransaction txn = totpTransactions.get(ip + "|" + account);
        if (txn == null) {
            return false;
        }
        if (System.currentTimeMillis() - txn.lastAttemptMs > TOTP_TRANSACTION_TTL) {
            totpTransactions.remove(ip + "|" + account);
            return false;
        }
        return txn.attempts >= TOTP_MAX_ATTEMPTS;
    }

    /** Record a TOTP failure within the post-password transaction. Returns the attempt count. */
    public int recordTotpFailure(String ip, String account) {
        long now = System.currentTimeMillis();
        TotpTransaction txn = totpTransactions.compute(ip + "|" + account, (k, existing) -> {
            if (existing == null || now - existing.lastAttemptMs > TOTP_TRANSACTION_TTL) {
                return new TotpTransaction(1, now);
            }
            existing.attempts++;
            existing.lastAttemptMs = now;
            return existing;
        });
        return txn.attempts;
    }

    private long activeBlock(String ip, String account, long now) {
        long until = 0;
        until = Math.max(until, blockUntil("ip:" + ip, now));
        if (account != null && !account.isBlank()) {
            until = Math.max(until, blockUntil("acc:" + account, now));
            until = Math.max(until, blockUntil("combo:" + ip + "|" + account, now));
        }
        return until;
    }

    private long blockUntil(String key, long now) {
        Long until = blockedUntil.get(key);
        if (until == null) {
            return 0;
        }
        if (until <= now) {
            blockedUntil.remove(key);
            return 0;
        }
        return until;
    }

    private void block(String key, long until) {
        blockedUntil.put(key, until);
    }

    private int record(ConcurrentHashMap<String, Deque<Long>> map, String key, long now, long window) {
        Deque<Long> log = map.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (log) {
            prune(log, now, window);
            log.addLast(now);
            return log.size();
        }
    }

    private int countWithin(Deque<Long> log, long now, long window) {
        if (log == null) {
            return 0;
        }
        synchronized (log) {
            prune(log, now, window);
            return log.size();
        }
    }

    private void prune(Deque<Long> log, long now, long window) {
        while (!log.isEmpty() && now - log.peekFirst() > window) {
            log.pollFirst();
        }
    }

    private static final class TotpTransaction {
        private int attempts;
        private long lastAttemptMs;

        private TotpTransaction(int attempts, long lastAttemptMs) {
            this.attempts = attempts;
            this.lastAttemptMs = lastAttemptMs;
        }
    }
}
