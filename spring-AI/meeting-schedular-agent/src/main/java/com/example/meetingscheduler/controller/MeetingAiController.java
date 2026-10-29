package com.example.meetingscheduler.controller;

import com.example.meetingscheduler.ai.IntentType;
import com.example.meetingscheduler.config.ConditionalOnGeminiApiKey;
import com.example.meetingscheduler.dto.AiMeetingResponse;
import com.example.meetingscheduler.dto.NaturalLanguageRequest;
import com.example.meetingscheduler.service.MeetingAiService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meetings")
@ConditionalOnGeminiApiKey
public class MeetingAiController {

    private final MeetingAiService meetingAiService;

    public MeetingAiController(MeetingAiService meetingAiService) {
        this.meetingAiService = meetingAiService;
    }

    @PostMapping("/ai")
    public ResponseEntity<AiMeetingResponse> processNaturalLanguage(@Valid @RequestBody NaturalLanguageRequest request) {
        AiMeetingResponse response = meetingAiService.processNaturalLanguage(request);
        return ResponseEntity.status(mapAiStatus(response)).body(response);
    }

    @PostMapping("/ai/tools")
    public ResponseEntity<String> processWithTools(@Valid @RequestBody NaturalLanguageRequest request) {
        return ResponseEntity.ok(meetingAiService.processWithTools(request));
    }

    private HttpStatus mapAiStatus(AiMeetingResponse response) {
        if (response.outcome() == IntentType.CLARIFICATION_REQUIRED) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        }
        if (response.conflicts() != null && !response.conflicts().isEmpty()) {
            return HttpStatus.CONFLICT;
        }
        if (response.outcome() == IntentType.SCHEDULE && response.meeting() != null) {
            return HttpStatus.CREATED;
        }
        if (response.outcome() == IntentType.CHECK_AVAILABILITY && response.availability() != null) {
            return response.availability().available() ? HttpStatus.OK : HttpStatus.CONFLICT;
        }
        return HttpStatus.OK;
    }
}
