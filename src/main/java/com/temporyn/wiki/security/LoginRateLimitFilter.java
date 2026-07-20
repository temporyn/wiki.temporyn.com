package com.temporyn.wiki.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces IP/account rate limits and temporary blocks on login POSTs before the
 * credentials are checked. Blocked or over-quota requests are logged and redirected
 * to a generic error page; otherwise a progressive delay may be applied.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final LoginAttemptService attempts;
    private final AuthEventLogger authEventLogger;

    public LoginRateLimitFilter(LoginAttemptService attempts, AuthEventLogger authEventLogger) {
        this.attempts = attempts;
        this.authEventLogger = authEventLogger;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!isLoginPost(request)) {
            chain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        String account = request.getParameter("username");

        LoginAttemptService.Decision decision = attempts.checkRequest(ip, account);
        if (!decision.allowed()) {
            authEventLogger.logRateLimited(request, account, decision.reason(), decision.blockedUntilEpochMs());
            response.sendRedirect(request.getContextPath() + "/login?error");
            return;
        }

        long delay = attempts.delayMillis(account);
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isLoginPost(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && "/login".equals(request.getServletPath());
    }
}
