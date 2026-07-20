package com.temporyn.wiki.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/** Logs a successful authentication before completing the redirect to the target page. */
@Component
public class AuthSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final AuthEventLogger authEventLogger;
    private final LoginAttemptService attempts;

    public AuthSuccessHandler(AuthEventLogger authEventLogger, LoginAttemptService attempts) {
        this.authEventLogger = authEventLogger;
        this.attempts = attempts;
        setDefaultTargetUrl("/");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        attempts.recordSuccess(request.getRemoteAddr(), authentication.getName());
        authEventLogger.logSuccess(request, authentication.getName(), "Password + TOTP");
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
