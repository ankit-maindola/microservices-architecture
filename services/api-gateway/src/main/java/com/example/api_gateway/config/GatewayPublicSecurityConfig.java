package com.example.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Auth login/register and static entry paths stay public; all other gateway routes
 * (including proxied /api/users/**) require session or JWT via common-security.
 */
@Configuration
public class GatewayPublicSecurityConfig {

	@Bean
	@Order(1)
	SecurityFilterChain gatewayPublicChain(HttpSecurity http) throws Exception {
		http
				.securityMatcher(
						"/",
						"/logout",
						"/api/auth/**",
						"/actuator/health",
						"/actuator/info")
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
		return http.build();
	}
}
