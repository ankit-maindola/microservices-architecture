package com.example.common.security;

import io.jsonwebtoken.security.Keys;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;
@AutoConfiguration
@EnableConfigurationProperties(AuthSecurityProperties.class)
public class AuthResourceServerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	AuthJwtValidator authJwtValidator(AuthSecurityProperties properties) {
		return new AuthJwtValidator(properties.getJwt().getSecret());
	}

	@Bean
	@ConditionalOnMissingBean
	AuthSessionValidator authSessionValidator(AuthSecurityProperties properties) {
		return new AuthSessionValidator(properties.getServiceUrl(), properties.getSessionCookieName());
	}

	@Bean
	@ConditionalOnMissingBean
	AuthJwtIssuer authJwtIssuer(AuthSecurityProperties properties) {
		var secretKey = Keys.hmacShaKeyFor(
				properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
		return new AuthJwtIssuer(secretKey, properties.getJwt().getExpirationMs());
	}

	@Bean
	@ConditionalOnMissingBean
	SessionBearerProvisioningFilter sessionBearerProvisioningFilter(AuthJwtIssuer jwtIssuer) {
		return new SessionBearerProvisioningFilter(jwtIssuer);
	}

	@Bean
	@ConditionalOnMissingBean
	AuthServiceAuthenticationFilter authServiceAuthenticationFilter(
			AuthJwtValidator jwtValidator,
			AuthSessionValidator sessionValidator
	) {
		return new AuthServiceAuthenticationFilter(jwtValidator, sessionValidator);
	}

	@Bean
	@ConditionalOnMissingBean(name = "resourceServerSecurityFilterChain")
	SecurityFilterChain resourceServerSecurityFilterChain(
			HttpSecurity http,
			AuthServiceAuthenticationFilter authFilter,
			SessionBearerProvisioningFilter sessionBearerFilter
	) throws Exception {
		http
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex.authenticationEntryPoint(
						new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/actuator/info").permitAll()
						.anyRequest().authenticated())
				.csrf(csrf -> csrf.disable())
				.addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(sessionBearerFilter, AuthServiceAuthenticationFilter.class);
		return http.build();
	}
}
