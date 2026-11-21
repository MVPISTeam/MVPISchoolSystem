package com.mountainview.sms.security;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // No browser cookies or sessions: every request carries its own username and password
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Lets the H2 console draw itself inside a frame
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        // Everything under /api needs a login. Which ROLE is decided per controller.
                        .requestMatchers("/api/**").authenticated()
                        // Swagger, the H2 console and any static pages are open
                        .anyRequest().permitAll())
                // HTTP Basic: the username and password travel in the Authorization header
                .httpBasic(basic -> basic.authenticationEntryPoint(SecurityConfig::writeUnauthorized));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

   // Missing or wrong login: answer 401 with a JSON message instead of the browser's own pop-up.
    private static void writeUnauthorized(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("""
                {"status":401,"error":"Unauthorized","message":"Log in with a valid username and password"}""");
    }
}
