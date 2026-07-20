package com.temporyn.wiki.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Logs a failed authentication (with the failing stage) then redirects to a generic
 * error page. The user-facing response never reveals which factor failed.
 */
@Component
public class AuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final AuthEventLogger authEventLogger;
    private final LoginAttemptService attempts;

    public AuthFailureHandler(AuthEventLogger authEventLogger, LoginAttemptService attempts) {
        super("/login?error");
        this.authEventLogger = authEventLogger;
        this.attempts = attempts;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String account = request.getParameter("username");
        String stage = (exception instanceof TotpAuthenticationException) ? "TOTP" : "PASSWORD";
        String autoBlock = attempts.recordFailure(request.getRemoteAddr(), account);
        authEventLogger.logFailure(request, account, stage, exception.getClass().getSimpleName(), "-", autoBlock);
        super.onAuthenticationFailure(request, response, exception);
    }
}
