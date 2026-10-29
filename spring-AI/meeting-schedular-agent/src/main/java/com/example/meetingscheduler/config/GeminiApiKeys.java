package com.example.meetingscheduler.config;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

public final class GeminiApiKeys {

    private GeminiApiKeys() {
    }

    public static String resolve(Environment environment) {
        for (String property : new String[] {
                "spring.ai.google.genai.api-key",
                "GEMINI_API_KEY",
                "GOOGLE_API_KEY",
                "meeting.gemini-api-key"
        }) {
            String value = environment.getProperty(property);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    public static boolean isConfigured(Environment environment) {
        return resolve(environment) != null;
    }
}
