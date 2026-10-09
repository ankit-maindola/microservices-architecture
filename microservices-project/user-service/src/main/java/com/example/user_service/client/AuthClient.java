package com.example.user_service.client;

import com.example.user_service.dto.AuthResponse;
import com.example.user_service.dto.LoginRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service", configuration = AuthFeignConfig.class)
public interface AuthClient {

	@PostMapping("/api/auth/public/login")
	AuthResponse login(@RequestBody LoginRequest request);
}
