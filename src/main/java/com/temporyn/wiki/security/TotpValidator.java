package com.temporyn.wiki.security;

import java.nio.ByteBuffer;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Time-based One-Time Password validator (RFC 6238, HMAC-SHA1, 6 digits, 30s step).
 * Implemented without external dependencies. The shared secret is Base32-encoded.
 */
@Component
public class TotpValidator {

    private static final int DIGITS = 6;
    private static final long STEP_SECONDS = 30;
    private static final int ALLOWED_DRIFT_STEPS = 1;

    /** Verify a user-entered code against the secret, tolerating +-1 time step for clock drift. */
    public boolean verify(String base32Secret, String code) {
        if (base32Secret == null || base32Secret.isBlank() || code == null) {
            return false;
        }
        String normalized = code.trim();
        if (!normalized.matches("\\d{" + DIGITS + "}")) {
            return false;
        }
        byte[] key;
        try {
            key = base32Decode(base32Secret);
        } catch (IllegalArgumentException e) {
            return false;
        }
        long currentStep = Instant.now().getEpochSecond() / STEP_SECONDS;
        for (int drift = -ALLOWED_DRIFT_STEPS; drift <= ALLOWED_DRIFT_STEPS; drift++) {
            if (normalized.equals(generate(key, currentStep + drift))) {
                return true;
            }
        }
        return false;
    }

    private String generate(byte[] key, long step) {
        byte[] data = ByteBuffer.allocate(Long.BYTES).putLong(step).array();
        byte[] hash;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            hash = mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot compute TOTP", e);
        }
        int offset = hash[hash.length - 1] & 0x0f;
        int binary = ((hash[offset] & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);
        int otp = binary % (int) Math.pow(10, DIGITS);
        return String.format("%0" + DIGITS + "d", otp);
    }

    /** Decode an RFC 4648 Base32 string (ignoring spaces, padding, and case). */
    private byte[] base32Decode(String encoded) {
        String clean = encoded.replace(" ", "").replace("=", "").toUpperCase();
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        int bits = 0;
        int value = 0;
        byte[] out = new byte[clean.length() * 5 / 8];
        int index = 0;
        for (int i = 0; i < clean.length(); i++) {
            int c = alphabet.indexOf(clean.charAt(i));
            if (c < 0) {
                throw new IllegalArgumentException("Invalid Base32 character");
            }
            value = (value << 5) | c;
            bits += 5;
            if (bits >= 8) {
                out[index++] = (byte) ((value >>> (bits - 8)) & 0xff);
                bits -= 8;
            }
        }
        return out;
    }
}
