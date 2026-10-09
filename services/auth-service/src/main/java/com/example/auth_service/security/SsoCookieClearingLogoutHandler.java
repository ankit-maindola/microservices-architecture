package com.example.auth_service.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

public class SsoCookieClearingLogoutHandler implements LogoutHandler {

	private static final String[] COOKIES_TO_CLEAR = {
			"AUTH_SESSION",
			"JSESSIONID",
			"USER_SERVICE_SESSION",
			"ORDER_SERVICE_SESSION"
	};

	@Override
	public void logout(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication
	) {
		for (String cookieName : COOKIES_TO_CLEAR) {
			expireCookie(response, cookieName);
		}
	}

	private static void expireCookie(HttpServletResponse response, String name) {
		Cookie cookie = new Cookie(name, "");
		cookie.setPath("/");
		cookie.setMaxAge(0);
		cookie.setHttpOnly(true);
		response.addCookie(cookie);
	}
}
