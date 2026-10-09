package com.example.user_service.client;

import com.example.common.security.AuthBearerForwardingContext;
import com.example.user_service.security.JwtForwardingContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class FeignJwtConfig {

	@Bean
	RequestInterceptor jwtForwardingInterceptor() {
		return this::applyJwtHeader;
	}

	private void applyJwtHeader(RequestTemplate template) {
		String authorization = resolveAuthorizationHeader();
		if (authorization != null && !authorization.isBlank()) {
			template.header(HttpHeaders.AUTHORIZATION, authorization);
		}
	}

	private String resolveAuthorizationHeader() {
		ServletRequestAttributes attributes =
				(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attributes != null) {
			HttpServletRequest request = attributes.getRequest();
			String incoming = request.getHeader(HttpHeaders.AUTHORIZATION);
			if (incoming != null && !incoming.isBlank()) {
				return incoming;
			}
		}
		String token = JwtForwardingContext.getBearerToken();
		if (token != null && !token.isBlank()) {
			return token.startsWith("Bearer ") ? token : "Bearer " + token;
		}
		token = AuthBearerForwardingContext.getBearerToken();
		if (token != null && !token.isBlank()) {
			return token.startsWith("Bearer ") ? token : "Bearer " + token;
		}
		return null;
	}
}
