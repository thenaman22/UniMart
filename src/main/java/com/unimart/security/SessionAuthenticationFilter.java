package com.unimart.security;

import com.unimart.api.AuthContext;
import com.unimart.api.CurrentUserArgumentResolver;
import com.unimart.domain.UserSession;
import com.unimart.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final UserSessionRepository userSessionRepository;

    public SessionAuthenticationFilter(UserSessionRepository userSessionRepository) {
        this.userSessionRepository = userSessionRepository;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String token = request.getHeader("X-Auth-Token");
        if (token != null && !token.isBlank()) {
            UserSession session = userSessionRepository.findByTokenAndExpiresAtAfter(token, Instant.now()).orElse(null);
            if (session != null) {
                request.setAttribute(CurrentUserArgumentResolver.AUTH_CONTEXT_ATTRIBUTE, new AuthContext(session.getUser()));
            }
        }
        filterChain.doFilter(request, response);
    }
}
