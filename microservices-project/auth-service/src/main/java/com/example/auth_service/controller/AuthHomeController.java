package com.example.auth_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthHomeController {

    @GetMapping("/api/auth/home")
    public String home(
            @AuthenticationPrincipal UserDetails user,
            Model model
    ) {
        if (user != null) {
            model.addAttribute("email", user.getUsername());
        }
        return "auth-home";
    }
}
