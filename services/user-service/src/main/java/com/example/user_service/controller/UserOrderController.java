package com.example.user_service.controller;

import com.example.user_service.client.OrderClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserOrderController {

	private final OrderClient orderClient;

	public UserOrderController(OrderClient orderClient) {
		this.orderClient = orderClient;
	}

	@GetMapping("/orders/{orderName}")
	public String placeOrder(@PathVariable String orderName) {
		return orderClient.placeOrder(orderName);
	}
}
