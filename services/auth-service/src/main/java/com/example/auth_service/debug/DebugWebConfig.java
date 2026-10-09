package com.example.auth_service.debug;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class DebugWebConfig implements WebMvcConfigurer {

    private final DebugLoginHandlerInterceptor loginHandlerInterceptor;

    public DebugWebConfig(DebugLoginHandlerInterceptor loginHandlerInterceptor) {
        this.loginHandlerInterceptor = loginHandlerInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginHandlerInterceptor)
                .addPathPatterns("/api/auth/public/login", "/api/auth/session/login");
    }
}
