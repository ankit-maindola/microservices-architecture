package com.example.auth_service.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.io.IOException;

/**
 * Always redirect to a context-relative URL so logout works through the API gateway (8080)
 * without sending the browser to a backend host/port (8081, internal IP, etc.).
 */
public class SsoLogoutSuccessHandler implements LogoutSuccessHandler {

	private static final String LOGOUT_SUCCESS_PATH = "/api/auth/home?logout";

	@Override
	public void onLogoutSuccess(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication
	) throws IOException {
		response.sendRedirect(LOGOUT_SUCCESS_PATH);
	}
}
