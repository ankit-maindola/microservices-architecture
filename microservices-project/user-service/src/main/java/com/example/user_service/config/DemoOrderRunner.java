package com.example.user_service.config;

import com.example.user_service.client.AuthClient;
import com.example.user_service.client.OrderClient;
import com.example.user_service.dto.LoginRequest;
import com.example.user_service.security.JwtForwardingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.demo.run-on-startup", havingValue = "true")
public class DemoOrderRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoOrderRunner.class);

	private final AuthClient authClient;
	private final OrderClient orderClient;
	private final String demoEmail;
	private final String demoPassword;

	public DemoOrderRunner(
			AuthClient authClient,
			OrderClient orderClient,
			@Value("${app.demo.email}") String demoEmail,
			@Value("${app.demo.password}") String demoPassword
	) {
		this.authClient = authClient;
		this.orderClient = orderClient;
		this.demoEmail = demoEmail;
		this.demoPassword = demoPassword;
	}

	@Override
	public void run(ApplicationArguments args) {
		var tokens = authClient.login(new LoginRequest(demoEmail, demoPassword));
		JwtForwardingContext.setBearerToken(tokens.accessToken());
		try {
			log.info("Feign order response: {}", orderClient.placeOrder("Order333"));
		} finally {
			JwtForwardingContext.clear();
		}
	}
}
