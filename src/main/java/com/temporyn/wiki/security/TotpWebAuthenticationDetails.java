package com.temporyn.wiki.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/** Captures the submitted TOTP code alongside the standard web authentication details. */
public class TotpWebAuthenticationDetails extends WebAuthenticationDetails {

    private final String code;

    public TotpWebAuthenticationDetails(HttpServletRequest request) {
        super(request);
        this.code = request.getParameter("code");
    }

    public String getCode() {
        return code;
    }

    /** Details source registered with the login filter to build the details above. */
    public static final class Source
            implements AuthenticationDetailsSource<HttpServletRequest, TotpWebAuthenticationDetails> {
        @Override
        public TotpWebAuthenticationDetails buildDetails(HttpServletRequest request) {
            return new TotpWebAuthenticationDetails(request);
        }
    }
}
