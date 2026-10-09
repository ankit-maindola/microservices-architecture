package com.example.auth_service.controller;
import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.RegisterRequest;
import com.example.auth_service.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> authApiInfo() {
        return ResponseEntity.ok(Map.of(
                "service", "auth-service",
                "endpoints", Map.of(
                        "browserLogin", "GET /api/auth/home (form login)",
                        "register", "POST /api/auth/register JSON: { email, password }",
                        "login", "POST /api/auth/login JSON: { email, password }"
                )
        ));
    }

    @GetMapping("login")
    public ResponseEntity<Map<String, String>> loginHelp() {
        return ResponseEntity.ok(Map.of(
                "method", "POST",
                "contentType", "application/json",
                "example", "{\"email\":\"user@example.com\",\"password\":\"password123\"}"
        ));
    }

    @PostMapping("public/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        return ResponseEntity.ok(
                authService.register(request)
        );
    }

    @PostMapping("public/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("me")
    public String currentUser(Authentication authentication) {
        return authentication.getName();
    }

    @GetMapping("session/welcome")
    public RedirectView welcomeSession() {
        return new RedirectView("/api/auth/home");
    }

    @GetMapping("session/me")
    public String getCurrentUsernameSession(Authentication authentication) {
        return authentication.getName();
    }

    @GetMapping("jwt/welcome")
    public String welcomeJwt() {
        return "Welcome! You are signed in.";
    }

    @GetMapping("jwt/me")
    public String getCurrentUsernameJwt(Authentication authentication) {
        return authentication.getName();
    }
}

