package com.example.meetingscheduler.controller;

import com.example.meetingscheduler.exception.ErrorResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.meetingscheduler.service.MeetingAiService;

import java.time.Instant;
import java.util.List;

/**
 * Registered when OpenAI is not configured. Prevents POST /api/meetings/ai from being
 * mistaken for POST /api/meetings/{id} (which produced HTTP 405).
 */
@RestController
@RequestMapping("/api/meetings")
@ConditionalOnMissingBean(MeetingAiService.class)
public class MeetingAiDisabledController {

    @PostMapping("/ai")
    public ResponseEntity<ErrorResponse> aiDisabled() {
        return disabledResponse();
    }

    @PostMapping("/ai/tools")
    public ResponseEntity<ErrorResponse> aiToolsDisabled() {
        return disabledResponse();
    }

    private ResponseEntity<ErrorResponse> disabledResponse() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(
                Instant.now(),
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "AI Not Configured",
                "Natural-language scheduling is disabled. Set GEMINI_API_KEY and restart, then check GET /api/meetings/ai/status.",
                List.of()));
    }
}
