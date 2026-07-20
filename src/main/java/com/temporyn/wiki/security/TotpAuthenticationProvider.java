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
    private final String totpSecret;

    public TotpAuthenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            TotpValidator totpValidator,
            @Value("${app.admin.totp-secret:}") String totpSecret) {
        super(userDetailsService);
        setPasswordEncoder(passwordEncoder);
        this.totpValidator = totpValidator;
        this.totpSecret = totpSecret;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication result = super.authenticate(authentication);

        if (totpSecret != null && !totpSecret.isBlank()) {
            String code = null;
            if (authentication.getDetails() instanceof TotpWebAuthenticationDetails details) {
                code = details.getCode();
            }
            if (!totpValidator.verify(totpSecret, code)) {
                throw new TotpAuthenticationException("Invalid authentication code.");
            }
        }
        return result;
    }
}
