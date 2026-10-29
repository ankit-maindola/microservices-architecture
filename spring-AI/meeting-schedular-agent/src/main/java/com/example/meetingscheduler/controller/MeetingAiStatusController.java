package com.example.meetingscheduler.controller;

import com.example.meetingscheduler.config.GeminiApiKeys;
import com.example.meetingscheduler.service.MeetingAiService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings/ai")
public class MeetingAiStatusController {

    private final ObjectProvider<MeetingAiService> meetingAiService;
    private final Environment environment;

    public MeetingAiStatusController(ObjectProvider<MeetingAiService> meetingAiService, Environment environment) {
        this.meetingAiService = meetingAiService;
        this.environment = environment;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        boolean keyConfigured = GeminiApiKeys.isConfigured(environment);
        boolean available = meetingAiService.getIfAvailable() != null;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("provider", "google-gemini");
        body.put("available", available);
        body.put("apiKeyConfigured", keyConfigured);
        body.put(
                "geminiPromptLogging",
                environment.getProperty("meeting.gemini.log-requests", Boolean.class, true));
        body.put("naturalLanguageEndpoint", available ? "/api/meetings/ai" : null);
        body.put("toolsEndpoint", available ? "/api/meetings/ai/tools" : null);
        if (available) {
            body.put("hint", "POST to the endpoints above with prompt, optional timeZone, confirmSchedule.");
        } else if (keyConfigured) {
            body.put("hint", "Gemini API key is set but AI beans did not start. Restart the app after changing env vars.");
        } else {
            body.put("hint", """
                    Set GEMINI_API_KEY (Google AI Studio key) in Run → Edit Configurations or .env, then restart.""");
        }
        return body;
    }
}
