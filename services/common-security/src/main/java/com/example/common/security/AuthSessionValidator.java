package com.example.common.security;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;

public class AuthSessionValidator {

	private static final Pattern EMAIL_LIKE = Pattern.compile("^[^\\s@<>]+@[^\\s@<>]+\\.[^\\s@<>]+$");

	private static final String SESSION_INTROSPECTION_HEADER = "X-Auth-Introspection";

	private final String authServiceBaseUrl;
	private final String sessionCookieName;
	private final HttpClient httpClient;

	public AuthSessionValidator(String authServiceBaseUrl, String sessionCookieName) {
		this.authServiceBaseUrl = trimTrailingSlash(authServiceBaseUrl);
		this.sessionCookieName = sessionCookieName == null || sessionCookieName.isBlank()
				? "AUTH_SESSION"
				: sessionCookieName;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(3))
				.followRedirects(HttpClient.Redirect.NEVER)
				.build();
	}

	public Optional<String> validateAndExtractUsername(String cookieHeader) {
		Optional<String> authCookie = AuthCookieSupport.authCookieHeader(cookieHeader, sessionCookieName);
		if (authCookie.isEmpty()) {
			return Optional.empty();
		}
		String cookieForAuth = authCookie.get();
		for (String path : new String[] { "/api/auth/session/me", "/api/auth/me" }) {
			Optional<String> username = fetchUsername(path, cookieForAuth);
			if (username.isPresent()) {
				return username;
			}
		}
		return Optional.empty();
	}

	private Optional<String> fetchUsername(String path, String cookieHeader) {
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(authServiceBaseUrl + path))
					.timeout(Duration.ofSeconds(3))
					.header("Cookie", cookieHeader)
					.header(SESSION_INTROSPECTION_HEADER, "true")
					.GET()
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				return Optional.empty();
			}
			String body = response.body();
			if (body == null) {
				return Optional.empty();
			}
			String username = body.trim();
			if (username.isBlank() || username.contains("<") || !EMAIL_LIKE.matcher(username).matches()) {
				return Optional.empty();
			}
			return Optional.of(username);
		} catch (Exception ex) {
			return Optional.empty();
		}
	}

	private static String trimTrailingSlash(String url) {
		if (url == null || url.isBlank()) {
			return "http://localhost:8081";
		}
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}
}
