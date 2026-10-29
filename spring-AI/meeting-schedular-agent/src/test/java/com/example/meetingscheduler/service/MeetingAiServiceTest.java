package com.example.meetingscheduler.service;

import com.example.meetingscheduler.ai.IntentType;
import com.example.meetingscheduler.ai.MeetingIntent;
import com.example.meetingscheduler.ai.MeetingIntentParser;
import com.example.meetingscheduler.ai.MeetingIntentValidator;
import com.example.meetingscheduler.ai.MeetingToolService;
import com.example.meetingscheduler.dto.NaturalLanguageRequest;
import com.example.meetingscheduler.exception.AiProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingAiServiceTest {

    @Mock
    private MeetingIntentParser meetingIntentParser;

    @Mock
    private MeetingService meetingService;

    @Mock
    private MeetingToolService meetingToolService;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    private MeetingAiService meetingAiService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.defaultSystem(anyString())).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        meetingAiService = new MeetingAiService(
                meetingIntentParser,
                new MeetingIntentValidator(),
                meetingService,
                meetingToolService,
                chatClientBuilder,
                Clock.fixed(Instant.parse("2026-10-28T10:00:00Z"), ZoneOffset.UTC),
                "UTC");
    }

    @Test
    void returnsClarificationWhenParserMarksMissingFields() {
        MeetingIntent intent = new MeetingIntent();
        intent.setIntentType(IntentType.CLARIFICATION_REQUIRED);
        intent.setMissingFields(List.of("startTime"));
        intent.setClarificationQuestion("When should the meeting start?");

        when(meetingIntentParser.parse(any(), any())).thenReturn(intent);

        var response = meetingAiService.processNaturalLanguage(
                new NaturalLanguageRequest("Schedule with Alice tomorrow", null, null, false));

        assertThat(response.outcome()).isEqualTo(IntentType.CLARIFICATION_REQUIRED);
        assertThat(response.clarificationQuestion()).contains("When should the meeting start");
    }

    @Test
    void propagatesAiParserFailures() {
        when(meetingIntentParser.parse(any(), any())).thenThrow(new AiProcessingException("down"));

        assertThatThrownBy(() -> meetingAiService.processNaturalLanguage(
                new NaturalLanguageRequest("hello", null, null, false)))
                .isInstanceOf(AiProcessingException.class);
    }
}
