package com.example.common.security;

import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.util.Date;

public class AuthJwtIssuer {

	private final SecretKey secretKey;
	private final long expirationMs;

	public AuthJwtIssuer(SecretKey secretKey, long expirationMs) {
		this.secretKey = secretKey;
		this.expirationMs = expirationMs;
	}

	public String generateAccessToken(String username) {
		Date now = new Date();
		return Jwts.builder()
				.subject(username)
				.issuedAt(now)
				.expiration(new Date(now.getTime() + expirationMs))
				.signWith(secretKey)
				.compact();
	}
}
