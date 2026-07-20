package com.temporyn.wiki.security;

import org.springframework.security.authentication.BadCredentialsException;

/**
 * Raised when the password is correct but the TOTP code is missing or invalid.
 * Used internally to distinguish the failing stage in the auth log; the
 * user-facing response stays generic and never reveals which factor failed.
 */
public class TotpAuthenticationException extends BadCredentialsException {

    public TotpAuthenticationException(String message) {
        super(message);
    }
}
