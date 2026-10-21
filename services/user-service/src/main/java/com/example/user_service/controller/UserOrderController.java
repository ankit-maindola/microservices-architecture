package com.example.user_service.controller;

import com.example.user_service.client.OrderClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserOrderController {

	private final OrderClient orderClient;

	public UserOrderController(OrderClient orderClient) {
		this.orderClient = orderClient;
	}

	@GetMapping({ "", "/" })
	public Map<String, Object> apiInfo() {
		return Map.of(
				"service", "user-service",
				"endpoints", Map.of(
						"placeOrder", "GET /api/users/orders/{orderName}"),
				"auth", Map.of(
						"browserLogin", "GET /api/auth/home (via gateway: same host/port)",
						"apiLogin", "POST /api/auth/public/login → Authorization: Bearer <token>",
						"example", "GET /api/users/orders/order333"));
	}

	@GetMapping("/orders/{orderName}")
	public String placeOrder(@PathVariable String orderName) {
		return orderClient.placeOrder(orderName);
	}
}
