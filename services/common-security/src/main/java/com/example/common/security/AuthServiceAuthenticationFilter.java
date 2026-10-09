package com.example.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class AuthServiceAuthenticationFilter extends OncePerRequestFilter {

	private final AuthJwtValidator jwtValidator;
	private final AuthSessionValidator sessionValidator;

	public AuthServiceAuthenticationFilter(
			AuthJwtValidator jwtValidator,
			AuthSessionValidator sessionValidator
	) {
		this.jwtValidator = jwtValidator;
		this.sessionValidator = sessionValidator;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain chain
	) throws ServletException, IOException {
		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			authenticateFromBearer(request).or(() -> authenticateFromSession(request))
					.ifPresent(auth -> SecurityContextHolder.getContext().setAuthentication(auth));
		}
		chain.doFilter(request, response);
	}

	private java.util.Optional<UsernamePasswordAuthenticationToken> authenticateFromBearer(
			HttpServletRequest request
	) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith("Bearer ")) {
			return java.util.Optional.empty();
		}
		String token = header.substring(7).trim();
		return jwtValidator.validateAndExtractUsername(token)
				.map(this::authenticationForUser);
	}

	private java.util.Optional<UsernamePasswordAuthenticationToken> authenticateFromSession(
			HttpServletRequest request
	) {
		return sessionValidator.validateAndExtractUsername(request.getHeader(HttpHeaders.COOKIE))
				.map(this::authenticationForUser);
	}

	private UsernamePasswordAuthenticationToken authenticationForUser(String username) {
		return new UsernamePasswordAuthenticationToken(
				username,
				null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}
}
