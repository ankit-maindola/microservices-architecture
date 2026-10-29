package com.example.meetingscheduler.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;

/**
 * Logs prompts sent to Gemini. Never logs API keys.
 */
public final class GeminiPromptLogger {

    private static final Logger log = LoggerFactory.getLogger(GeminiPromptLogger.class);

    private GeminiPromptLogger() {
    }

    public static void logIntentParseRequest(
            boolean enabled,
            GoogleGenAiChatOptions options,
            String systemText,
            String userText) {
        if (!enabled) {
            return;
        }
        log.info(
                """
                ========== Gemini request (intent parse) ==========
                options: {}
                ----- SYSTEM -----
                {}
                ----- USER -----
                {}
                ==================================================
                """,
                options,
                systemText,
                userText);
    }

    public static void logIntentParseResponse(boolean enabled, String responseText) {
        if (!enabled || !log.isInfoEnabled()) {
            return;
        }
        log.info(
                """
                ========== Gemini response (intent parse) ==========
                {}
                ===================================================
                """,
                responseText);
    }
}
