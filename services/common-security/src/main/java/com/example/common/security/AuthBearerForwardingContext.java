package com.example.common.security;

public final class AuthBearerForwardingContext {

	private static final ThreadLocal<String> BEARER_TOKEN = new ThreadLocal<>();

	private AuthBearerForwardingContext() {
	}

	public static void setBearerToken(String accessToken) {
		BEARER_TOKEN.set(accessToken);
	}

	public static String getBearerToken() {
		return BEARER_TOKEN.get();
	}

	public static void clear() {
		BEARER_TOKEN.remove();
	}
}
