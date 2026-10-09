package com.example.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

public class AuthJwtValidator {

	private final SecretKey secretKey;

	public AuthJwtValidator(String secret) {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	public Optional<String> validateAndExtractUsername(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(secretKey)
					.build()
					.parseSignedClaims(token)
					.getPayload();
			if (claims.getExpiration().before(new Date())) {
				return Optional.empty();
			}
			return Optional.ofNullable(claims.getSubject());
		} catch (Exception ex) {
			return Optional.empty();
		}
	}
}
