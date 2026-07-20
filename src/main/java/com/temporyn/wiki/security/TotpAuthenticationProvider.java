package com.temporyn.wiki.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Authenticates username + password and, when a TOTP secret is configured,
 * additionally requires a valid time-based one-time code. When no secret is set
 * (e.g. local development) the TOTP step is skipped.
 */
@Component
public class TotpAuthenticationProvider extends DaoAuthenticationProvider {

    private final TotpValidator totpValidator;
    private final LoginAttemptService attempts;
    private final String totpSecret;

    public TotpAuthenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            TotpValidator totpValidator,
            LoginAttemptService attempts,
            @Value("${app.admin.totp-secret:}") String totpSecret) {
        super(userDetailsService);
        setPasswordEncoder(passwordEncoder);
        this.totpValidator = totpValidator;
        this.attempts = attempts;
        this.totpSecret = totpSecret;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication result = super.authenticate(authentication);

        if (totpSecret != null && !totpSecret.isBlank()) {
            String ip = null;
            String code = null;
            if (authentication.getDetails() instanceof TotpWebAuthenticationDetails details) {
                code = details.getCode();
                ip = details.getRemoteAddress();
            }
            String account = authentication.getName();

            // Reject once the post-password TOTP transaction has used up its attempts (expires after 5 min).
            if (ip != null && attempts.isTotpTransactionExhausted(ip, account)) {
                throw new TotpAuthenticationException("Too many authentication code attempts.");
            }
            if (!totpValidator.verify(totpSecret, code)) {
                if (ip != null) {
                    attempts.recordTotpFailure(ip, account);
                }
                throw new TotpAuthenticationException("Invalid authentication code.");
            }
        }
        return result;
    }
}

