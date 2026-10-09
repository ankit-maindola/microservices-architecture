package com.example.api_gateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Ensures downstream services see the public gateway host when generating redirects.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayForwardedHeadersFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String host = request.getHeader("Host");
		if (host == null || host.isBlank()) {
			filterChain.doFilter(request, response);
			return;
		}
		String proto = request.getScheme();
		String forwarded = "proto=" + proto + ";host=" + host;
		HttpServletRequest wrapped = new HttpServletRequestWrapper(request) {
			@Override
			public String getHeader(String name) {
				if ("Forwarded".equalsIgnoreCase(name)) {
					return forwarded;
				}
				if ("X-Forwarded-Host".equalsIgnoreCase(name)) {
					return host;
				}
				if ("X-Forwarded-Proto".equalsIgnoreCase(name)) {
					return proto;
				}
				return super.getHeader(name);
			}
		};
		filterChain.doFilter(wrapped, response);
	}
}
