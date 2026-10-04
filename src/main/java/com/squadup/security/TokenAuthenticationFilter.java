package com.squadup.security;

import com.squadup.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads {@code Authorization: Bearer <token>} and, if the token is valid, marks the request
 * as authenticated. Requests without a (valid) token continue anonymously and are rejected
 * later by the authorization rules in {@code SecurityConfig} when the endpoint needs a login.
 */
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    public TokenAuthenticationFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = TokenService.extractBearer(request.getHeader("Authorization"));
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            tokenService.authenticate(token).ifPresent(user -> {
                var auth = new UsernamePasswordAuthenticationToken(
                        user, token, AuthorityUtils.createAuthorityList("ROLE_USER"));
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(request, response);
    }
}
