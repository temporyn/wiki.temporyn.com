package com.temporyn.wiki.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private static final String H2_CONSOLE_PATH = "/h2-console/**";

    private static final String[] PUBLIC_PATHS = {
            "/",
            "/index.html",
            "/about/**",
            "/error",
            "/favicon.ico",
            "/assets/**",
            "/login",
            H2_CONSOLE_PATH
    };

    private static final String ADMIN_PAGE_PATH = "/admin/**";

    private static final String ADMIN_API_PATH = "/api/admin/**";

    private static final String USER_API_PATH = "/api/**";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers(ADMIN_API_PATH).hasRole("ADMIN")
                        .requestMatchers(ADMIN_PAGE_PATH).hasRole("ADMIN")
                        .requestMatchers(USER_API_PATH).permitAll()
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/admin", false)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll())
                .httpBasic(Customizer.withDefaults())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .csrf(csrf -> csrf.ignoringRequestMatchers(USER_API_PATH, H2_CONSOLE_PATH));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
