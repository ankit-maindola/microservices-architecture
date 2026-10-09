package com.example.auth_service.security;

import com.example.auth_service.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {

	private static final String SESSION_INTROSPECTION_HEADER = "X-Auth-Introspection";

	private static final RequestMatcher PUBLIC_AUTH_MATCHER = request -> {
		String uri = request.getRequestURI();
		if (uri == null || uri.isBlank()) {
			return false;
		}
		return uri.equals("/api/auth")
				|| uri.equals("/api/auth/login")
				|| uri.startsWith("/api/auth/public/");
	};

	private static final RequestMatcher LOGOUT_MATCHER = request -> {
		String uri = request.getRequestURI();
		String method = request.getMethod();
		if ("POST".equals(method) && ("/logout".equals(uri) || "/api/auth/session/logout".equals(uri))) {
			return true;
		}
		return "GET".equals(method) && "/api/auth/session/logout".equals(uri);
	};

	private final UserRepository userRepository;

	public SecurityConfig(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Bean
	UserDetailsService userDetailsService() {
		return email -> userRepository.findByEmail(email)
				.map(user -> org.springframework.security.core.userdetails.User
						.withUsername(user.getEmail())
						.password(user.getPassword())
						.roles(user.getRole().name())
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
		var provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	JwtAuthenticationFilter jwtAuthenticationFilter(
			JwtService jwtService,
			UserDetailsService userDetailsService
	) {
		return new JwtAuthenticationFilter(jwtService, userDetailsService);
	}

	@Bean
	@Order(1)
	SecurityFilterChain publicLoginChain(HttpSecurity http) throws Exception {
		http.securityMatcher(PUBLIC_AUTH_MATCHER)
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
		return http.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain browserSessionChain(HttpSecurity http, AuthenticationProvider authProvider)
			throws Exception {
		http.securityMatcher("/api/auth/session/**", "/logout", "/api/auth/home")
				.authenticationProvider(authProvider)
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
					if ("true".equalsIgnoreCase(request.getHeader(SESSION_INTROSPECTION_HEADER))) {
						response.sendError(HttpStatus.UNAUTHORIZED.value());
						return;
					}
					new org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint(
							"/api/auth/home").commence(request, response, authException);
				}))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/auth/session/login", "/api/auth/home").permitAll()
						.anyRequest().authenticated())
				.formLogin(form -> form
						.loginPage("/api/auth/home")
						.loginProcessingUrl("/api/auth/session/login")
						.defaultSuccessUrl("/api/auth/home", true)
						.failureUrl("/api/auth/home?error")
						.permitAll())
				.logout(logout -> logout
						.logoutRequestMatcher(LOGOUT_MATCHER)
						.invalidateHttpSession(true)
						.clearAuthentication(true)
						.deleteCookies("AUTH_SESSION", "JSESSIONID", "USER_SERVICE_SESSION", "ORDER_SERVICE_SESSION")
						.addLogoutHandler(new SsoCookieClearingLogoutHandler())
						.logoutSuccessHandler(new SsoLogoutSuccessHandler()));
		return http.build();
	}

	@Bean
	@Order(3)
	SecurityFilterChain securedAuthApiChain(
			HttpSecurity http,
			JwtAuthenticationFilter jwtFilter
	) throws Exception {
		http.securityMatcher("/api/auth/**")
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.exceptionHandling(ex -> ex.authenticationEntryPoint(
						new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(PUBLIC_AUTH_MATCHER).permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	@Order(4)
	SecurityFilterChain securedDefaultChain(
			HttpSecurity http,
			JwtAuthenticationFilter jwtFilter
	) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.exceptionHandling(ex -> ex.authenticationEntryPoint(
						new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
