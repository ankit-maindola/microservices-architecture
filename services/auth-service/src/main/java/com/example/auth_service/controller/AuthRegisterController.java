package com.example.auth_service.controller;

import com.example.auth_service.dto.RegisterRequest;
import com.example.auth_service.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthRegisterController {

	private final AuthService authService;

	public AuthRegisterController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping("/api/auth/register")
	public String registerForm() {
		return "auth-register";
	}

	@PostMapping("/api/auth/register")
	public String registerSubmit(
			@RequestParam String email,
			@RequestParam String password,
			@RequestParam String confirmPassword,
			RedirectAttributes redirectAttributes
	) {
		if (email == null || email.isBlank()) {
			redirectAttributes.addFlashAttribute("error", "Email is required.");
			return "redirect:/api/auth/register";
		}
		if (password == null || password.length() < 8) {
			redirectAttributes.addFlashAttribute("error", "Password must be at least 8 characters.");
			redirectAttributes.addFlashAttribute("email", email.trim());
			return "redirect:/api/auth/register";
		}
		if (!password.equals(confirmPassword)) {
			redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
			redirectAttributes.addFlashAttribute("email", email.trim());
			return "redirect:/api/auth/register";
		}
		try {
			authService.register(new RegisterRequest(email.trim(), password));
			return "redirect:/api/auth/home?registered=true";
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			redirectAttributes.addFlashAttribute("email", email.trim());
			return "redirect:/api/auth/register";
		}
	}
}
