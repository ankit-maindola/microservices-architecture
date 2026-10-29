package com.example.meetingscheduler.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
/**
 * Applies a custom JVM trust store before HTTP clients (Gemini) are created.
 * Use when a corporate proxy performs TLS inspection (PKIX / certificate_unknown errors).
 */
public class SslTrustStoreEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        JvmTrustStoreConfigurer.ensureConfigured(environment);
    }
}
