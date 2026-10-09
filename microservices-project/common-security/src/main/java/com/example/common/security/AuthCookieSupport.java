package com.example.common.security;

import java.util.Optional;

public final class AuthCookieSupport {

	private AuthCookieSupport() {
	}

	public static Optional<String> extractCookieValue(String cookieHeader, String cookieName) {
		if (cookieHeader == null || cookieHeader.isBlank() || cookieName == null || cookieName.isBlank()) {
			return Optional.empty();
		}
		for (String part : cookieHeader.split(";")) {
			String trimmed = part.trim();
			int eq = trimmed.indexOf('=');
			if (eq <= 0) {
				continue;
			}
			String name = trimmed.substring(0, eq).trim();
			if (name.equals(cookieName)) {
				return Optional.of(trimmed.substring(eq + 1).trim());
			}
		}
		return Optional.empty();
	}

	public static Optional<String> authCookieHeader(String cookieHeader, String cookieName) {
		return extractCookieValue(cookieHeader, cookieName)
				.map(value -> cookieName + "=" + value);
	}
}
