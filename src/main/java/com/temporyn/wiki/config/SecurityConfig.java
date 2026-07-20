package com.temporyn.wiki.config;

import com.temporyn.wiki.security.AuthFailureHandler;
import com.temporyn.wiki.security.AuthSuccessHandler;
import com.temporyn.wiki.security.LoginRateLimitFilter;
import com.temporyn.wiki.security.TotpAuthenticationProvider;
import com.temporyn.wiki.security.TotpWebAuthenticationDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/error",
            "/favicon.ico",
            "/assets/**",
            "/lib/**",
            "/login"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, TotpAuthenticationProvider totpProvider,
            AuthSuccessHandler successHandler, AuthFailureHandler failureHandler,
            LoginRateLimitFilter rateLimitFilter) throws Exception {
        http
                .authenticationProvider(totpProvider)
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .formLogin(form -> form
                        .loginPage("/login")
                        .authenticationDetailsSource(new TotpWebAuthenticationDetails.Source())
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login")
                        .permitAll())
                .headers(headers -> headers
                        // HSTS is safe to send because Nginx terminates TLS in front of the app.
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .contentTypeOptions(opts -> {
                        })
                        .frameOptions(frame -> frame.sameOrigin())
                        .referrerPolicy(ref -> ref
                                .policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));
        return http.build();
    }

    /** The single admin account is configured via environment variables; the password is a BCrypt hash. */
    @Bean
    UserDetailsService userDetailsService(
            @Value("${app.admin.username}") String username,
            @Value("${app.admin.password-hash}") String passwordHash) {
        return new InMemoryUserDetailsManager(
                User.withUsername(username)
                        .password(passwordHash)
                        .roles("ADMIN")
                        .build());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
