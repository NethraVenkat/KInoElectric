package com.backend.javaleap.service;

import com.backend.javaleap.model.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Authenticates machine clients sending {@code X-API-KEY}. Runs before the JWT filter.
 * Invalid keys leave the request anonymous, so protected endpoints answer 401.
 * Not a Spring bean on purpose (avoids double registration as a servlet filter).
 */
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader(ApiKeyService.HEADER);
        if (key != null && !key.isBlank() && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<UserPrincipal> principal = apiKeyService.authenticate(key.trim());
            principal.ifPresent(p -> {
                var auth = new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(request, response);
    }
}
