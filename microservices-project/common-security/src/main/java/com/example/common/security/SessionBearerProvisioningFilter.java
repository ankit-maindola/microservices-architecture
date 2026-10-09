package com.example.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * After session-based authentication, mint a short-lived JWT for downstream Feign calls.
 */
public class SessionBearerProvisioningFilter extends OncePerRequestFilter {

	private final AuthJwtIssuer jwtIssuer;

	public SessionBearerProvisioningFilter(AuthJwtIssuer jwtIssuer) {
		this.jwtIssuer = jwtIssuer;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain chain
	) throws ServletException, IOException {
		try {
			provisionBearerIfNeeded(request);
			chain.doFilter(request, response);
		} finally {
			AuthBearerForwardingContext.clear();
		}
	}

	private void provisionBearerIfNeeded(HttpServletRequest request) {
		String incomingAuth = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (incomingAuth != null && incomingAuth.startsWith("Bearer ")) {
			return;
		}
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return;
		}
		String username = authentication.getName();
		if (username == null || username.isBlank()) {
			return;
		}
		String token = jwtIssuer.generateAccessToken(username);
		AuthBearerForwardingContext.setBearerToken("Bearer " + token);
	}
}
