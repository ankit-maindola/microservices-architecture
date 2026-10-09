package com.example.auth_service.debug;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DebugAuthTraceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (uri.contains("/api/auth/public/login") || uri.contains("/api/auth/session/login")) {
            // #region agent log
            boolean authenticated = SecurityContextHolder.getContext().getAuthentication() != null
                    && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                    && !"anonymousUser".equals(
                    SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            DebugLog.write(
                    "H1-H3",
                    "DebugAuthTraceFilter:preChain",
                    "login POST before filter chain",
                    "{\"uri\":\"" + uri + "\",\"method\":\"" + request.getMethod()
                            + "\",\"contentType\":\"" + request.getContentType()
                            + "\",\"authenticatedBeforeChain\":" + authenticated + "}"
            );
            // #endregion
        }

        filterChain.doFilter(request, response);

        if (uri.contains("/api/auth/public/login") || uri.contains("/api/auth/session/login")) {
            // #region agent log
            DebugLog.write(
                    "H1",
                    "DebugAuthTraceFilter:postChain",
                    "login POST after filter chain",
                    "{\"uri\":\"" + uri + "\",\"status\":" + response.getStatus() + "}"
            );
            // #endregion
        }
    }
}
