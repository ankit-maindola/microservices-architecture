package com.example.common.security;

import java.util.List;

public final class SsoCookieNames {

	public static final String AUTH_SESSION = "AUTH_SESSION";
	public static final String USER_SERVICE_SESSION = "USER_SERVICE_SESSION";
	public static final String ORDER_SERVICE_SESSION = "ORDER_SERVICE_SESSION";
	public static final String LEGACY_JSESSIONID = "JSESSIONID";

	public static final List<String> ALL_SSO_COOKIES = List.of(
			AUTH_SESSION,
			LEGACY_JSESSIONID,
			USER_SERVICE_SESSION,
			ORDER_SERVICE_SESSION
	);

	private SsoCookieNames() {
	}
}
