package com.example.meetingscheduler.service;

import com.example.meetingscheduler.config.ConditionalOnGeminiApiKey;
import com.example.meetingscheduler.ai.MeetingIntent;
import com.example.meetingscheduler.ai.MeetingIntentParser;
import com.example.meetingscheduler.ai.MeetingIntentValidator;
import com.example.meetingscheduler.ai.MeetingToolService;
import com.example.meetingscheduler.dto.AiMeetingResponse;
import com.example.meetingscheduler.dto.AvailabilityRequest;
import com.example.meetingscheduler.dto.CreateMeetingRequest;
import com.example.meetingscheduler.dto.MeetingResponse;
import com.example.meetingscheduler.dto.NaturalLanguageRequest;
import com.example.meetingscheduler.dto.ParticipantDto;
import com.example.meetingscheduler.dto.UpdateMeetingRequest;
import com.example.meetingscheduler.exception.AiProcessingException;
import com.example.meetingscheduler.exception.SchedulingConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnGeminiApiKey
public class MeetingAiService {

    private static final Logger log = LoggerFactory.getLogger(MeetingAiService.class);

    private final MeetingIntentParser meetingIntentParser;
    private final MeetingIntentValidator meetingIntentValidator;
    private final MeetingService meetingService;
    private final MeetingToolService meetingToolService;
    private final ChatClient agentChatClient;
    private final ZoneId defaultTimeZone;
    private final Clock clock;

    public MeetingAiService(
            MeetingIntentParser meetingIntentParser,
            MeetingIntentValidator meetingIntentValidator,
            MeetingService meetingService,
            MeetingToolService meetingToolService,
            ChatClient.Builder chatClientBuilder,
            Clock clock,
            @Value("${meeting.default-time-zone:UTC}") String defaultTimeZone) {
        this.meetingIntentParser = meetingIntentParser;
        this.meetingIntentValidator = meetingIntentValidator;
        this.meetingService = meetingService;
        this.meetingToolService = meetingToolService;
        this.clock = clock;
        this.defaultTimeZone = ZoneId.of(defaultTimeZone);
        this.agentChatClient = chatClientBuilder
                .defaultSystem("""
                        You are a meeting scheduling assistant.
                        Use tools for availability checks, lookups, scheduling, and cancellation.
                        Never invent email addresses.
                        Before scheduling or cancelling, ensure the user explicitly requested the action.
                        For scheduleMeeting and cancelMeeting tools, pass confirmed=true only when the user clearly confirmed.
                        """)
                .build();
    }

    public AiMeetingResponse processNaturalLanguage(NaturalLanguageRequest request) {
        long started = System.nanoTime();
        ZoneId zoneId = resolveZoneId(request.timeZone());
        MeetingIntent rawIntent = meetingIntentParser.parse(request.prompt(), zoneId);
        MeetingIntentValidator.ResolvedMeetingIntent intent =
                meetingIntentValidator.resolve(rawIntent, zoneId);

        AiMeetingResponse response;
        if (intent.clarificationRequired()) {
            response = AiMeetingResponse.clarification(intent.missingFields(), intent.clarificationQuestion());
        } else if (intent.invalidMeetingId()) {
            response = AiMeetingResponse.clarification(List.of("meetingId"), "Please provide a valid meeting UUID.");
        } else {
            response = switch (intent.intentType()) {
                case SCHEDULE -> scheduleFromIntent(intent, request.createdBy(), request.confirmSchedule());
                case CHECK_AVAILABILITY -> AiMeetingResponse.availability(meetingService.checkAvailability(
                        new AvailabilityRequest(
                                intent.participants().stream().map(ParticipantDto::email).toList(),
                                intent.startTime(),
                                intent.endTime())));
                case LIST_MEETINGS -> listFromIntent(intent);
                case CANCEL_MEETING -> cancelFromIntent(intent, request.confirmSchedule());
                case UPDATE_MEETING -> updateFromIntent(intent, request.confirmSchedule());
                case CLARIFICATION_REQUIRED -> AiMeetingResponse.clarification(
                        intent.missingFields(), intent.clarificationQuestion());
            };
        }
        log.info(
                "Natural-language request completed in {} ms (outcome={})",
                (System.nanoTime() - started) / 1_000_000L,
                response.outcome());
        return response;
    }

    public String processWithTools(NaturalLanguageRequest request) {
        try {
            return agentChatClient.prompt()
                    .user(request.prompt())
                    .tools(meetingToolService)
                    .call()
                    .content();
        } catch (Exception ex) {
            throw new AiProcessingException("Tool-based AI request failed.", ex);
        }
    }

    private AiMeetingResponse scheduleFromIntent(
            MeetingIntentValidator.ResolvedMeetingIntent intent,
            String createdBy,
            boolean confirmSchedule) {
        if (!confirmSchedule) {
            var availability = meetingService.checkAvailability(new AvailabilityRequest(
                    intent.participants().stream().map(ParticipantDto::email).toList(),
                    intent.startTime(),
                    intent.endTime()));
            if (!availability.available()) {
                return AiMeetingResponse.conflicts(availability.conflicts());
            }
            return AiMeetingResponse.clarification(
                    List.of("confirmSchedule"),
                    "I can schedule \"" + intent.topic() + "\" at the requested time. Reply with confirmSchedule=true to proceed.");
        }
        try {
            MeetingResponse meeting = meetingService.createMeeting(new CreateMeetingRequest(
                    intent.topic(),
                    intent.description(),
                    intent.startTime(),
                    intent.endTime(),
                    intent.participants(),
                    createdBy));
            return AiMeetingResponse.scheduled(meeting);
        } catch (SchedulingConflictException ex) {
            return AiMeetingResponse.conflicts(ex.getConflicts());
        }
    }

    private AiMeetingResponse listFromIntent(MeetingIntentValidator.ResolvedMeetingIntent intent) {
        Instant from = intent.listFrom() != null ? intent.listFrom() : clock.instant().truncatedTo(ChronoUnit.DAYS);
        Instant to = intent.listTo() != null ? intent.listTo() : from.plus(7, ChronoUnit.DAYS);
        List<MeetingResponse> meetings;
        if (intent.listParticipantEmail() != null) {
            meetings = meetingService.listMeetingsForParticipant(intent.listParticipantEmail(), from, to);
        } else {
            meetings = meetingService.listMeetings(from, to);
        }
        return AiMeetingResponse.listed(meetings);
    }

    private AiMeetingResponse cancelFromIntent(MeetingIntentValidator.ResolvedMeetingIntent intent, boolean confirmSchedule) {
        UUID meetingId = intent.meetingId().orElseThrow(() ->
                new IllegalArgumentException("Meeting id is required to cancel."));
        if (!confirmSchedule) {
            MeetingResponse meeting = meetingService.getMeeting(meetingId);
            return AiMeetingResponse.clarification(
                    List.of("confirmSchedule"),
                    "Confirm cancellation of meeting \"" + meeting.topic() + "\" (" + meeting.id() + ").");
        }
        return AiMeetingResponse.cancelled(meetingService.cancelMeeting(meetingId));
    }

    private AiMeetingResponse updateFromIntent(MeetingIntentValidator.ResolvedMeetingIntent intent, boolean confirmSchedule) {
        UUID meetingId = intent.meetingId().orElseThrow(() ->
                new IllegalArgumentException("Meeting id is required to update."));
        if (!confirmSchedule) {
            return AiMeetingResponse.clarification(
                    List.of("confirmSchedule"),
                    "Confirm update for meeting " + meetingId + ".");
        }
        try {
            MeetingResponse meeting = meetingService.updateMeeting(meetingId, new UpdateMeetingRequest(
                    intent.topic(),
                    intent.description(),
                    intent.startTime(),
                    intent.endTime(),
                    intent.participants()));
            return AiMeetingResponse.scheduled(meeting);
        } catch (SchedulingConflictException ex) {
            return AiMeetingResponse.conflicts(ex.getConflicts());
        }
    }

    private ZoneId resolveZoneId(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return defaultTimeZone;
        }
        return ZoneId.of(timeZone.trim());
    }
}
