package com.example.user_service.client;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

public class AuthFeignConfig {

	@Bean
	RequestInterceptor authClientInterceptor() {
		return template -> template.removeHeader(HttpHeaders.AUTHORIZATION);
	}
}
