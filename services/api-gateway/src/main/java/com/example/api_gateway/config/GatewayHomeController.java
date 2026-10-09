package com.example.api_gateway.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class GatewayHomeController {

	@GetMapping("/")
	public RedirectView home() {
		return new RedirectView("/api/auth/home");
	}
}
