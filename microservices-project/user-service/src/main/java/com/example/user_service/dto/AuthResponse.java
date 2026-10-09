package com.example.user_service.dto;

public record AuthResponse(
		String accessToken,
		String refreshToken,
		String tokenType
) {
}
