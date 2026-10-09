package com.example.api_gateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;

/**
 * Rewrites absolute Location headers from downstream services (8081–8085, internal IPs)
 * back to the gateway host the browser used (e.g. localhost:8080).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class GatewayRedirectRewriteFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		HttpServletResponse wrapped = new HttpServletResponseWrapper(response) {
			@Override
			public void sendRedirect(String location) throws IOException {
				super.sendRedirect(rewriteLocation(request, location));
			}

			@Override
			public void setHeader(String name, String value) {
				if ("Location".equalsIgnoreCase(name)) {
					value = rewriteLocation(request, value);
				}
				super.setHeader(name, value);
			}

			@Override
			public void addHeader(String name, String value) {
				if ("Location".equalsIgnoreCase(name)) {
					value = rewriteLocation(request, value);
				}
				super.addHeader(name, value);
			}
		};
		filterChain.doFilter(request, wrapped);
	}

	static String rewriteLocation(HttpServletRequest request, String location) {
		if (location == null || location.isBlank()) {
			return location;
		}
		if (location.startsWith("/")) {
			return location;
		}
		try {
			URI uri = URI.create(location);
			String path = uri.getRawPath();
			if (path == null || path.isBlank()) {
				return location;
			}
			if (!path.startsWith("/api/") && !path.equals("/logout")) {
				return location;
			}
			String host = request.getHeader("Host");
			if (host == null || host.isBlank()) {
				int port = request.getServerPort();
				boolean defaultPort = "http".equalsIgnoreCase(request.getScheme()) && port == 80
						|| "https".equalsIgnoreCase(request.getScheme()) && port == 443;
				host = request.getServerName() + (defaultPort ? "" : ":" + port);
			}
			String query = uri.getRawQuery();
			return request.getScheme() + "://" + host + path + (query != null ? "?" + query : "");
		} catch (IllegalArgumentException ex) {
			return location;
		}
	}
}
