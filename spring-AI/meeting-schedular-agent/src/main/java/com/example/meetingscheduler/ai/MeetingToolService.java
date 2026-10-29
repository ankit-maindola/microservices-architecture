package com.example.meetingscheduler.ai;

import com.example.meetingscheduler.dto.AvailabilityRequest;
import com.example.meetingscheduler.dto.AvailabilityResponse;
import com.example.meetingscheduler.dto.CreateMeetingRequest;
import com.example.meetingscheduler.dto.MeetingResponse;
import com.example.meetingscheduler.dto.ParticipantDto;
import com.example.meetingscheduler.exception.SchedulingConflictException;
import com.example.meetingscheduler.service.MeetingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class MeetingToolService {

    private final MeetingService meetingService;
    private final ObjectMapper objectMapper;

    public MeetingToolService(MeetingService meetingService, ObjectMapper objectMapper) {
        this.meetingService = meetingService;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "Check whether participants are available between startTime and endTime (ISO-8601 instants).")
    public String checkAvailability(
            @ToolParam(description = "Participant email addresses") List<String> participantEmails,
            @ToolParam(description = "Start time as ISO-8601 instant") String startTime,
            @ToolParam(description = "End time as ISO-8601 instant") String endTime) {
        AvailabilityResponse response = meetingService.checkAvailability(new AvailabilityRequest(
                participantEmails,
                Instant.parse(startTime),
                Instant.parse(endTime)));
        return toJson(response);
    }

    @Tool(description = "Find meetings in a date range. Times must be ISO-8601 instants.")
    public String findMeetings(
            @ToolParam(description = "Range start inclusive") String from,
            @ToolParam(description = "Range end exclusive") String to) {
        List<MeetingResponse> meetings = meetingService.listMeetings(Instant.parse(from), Instant.parse(to));
        return toJson(meetings);
    }

    @Tool(description = "Get meeting details by UUID.")
    public String getMeetingDetails(@ToolParam(description = "Meeting UUID") String meetingId) {
        MeetingResponse meeting = meetingService.getMeeting(UUID.fromString(meetingId));
        return toJson(meeting);
    }

    @Tool(description = """
            Schedule a meeting. This persists data only when confirmed=true.
            Always validate availability first. Never set confirmed=true unless the user explicitly asked to schedule.
            """)
    public String scheduleMeeting(
            @ToolParam(description = "Meeting topic") String topic,
            @ToolParam(description = "Optional description", required = false) String description,
            @ToolParam(description = "Start time ISO-8601 instant") String startTime,
            @ToolParam(description = "End time ISO-8601 instant") String endTime,
            @ToolParam(description = "Participant emails") List<String> participantEmails,
            @ToolParam(description = "Must be true to persist the meeting") boolean confirmed,
            @ToolParam(description = "Optional organizer id", required = false) String createdBy) {
        if (!confirmed) {
            AvailabilityResponse availability = meetingService.checkAvailability(new AvailabilityRequest(
                    participantEmails, Instant.parse(startTime), Instant.parse(endTime)));
            return toJson(new ToolSchedulePreview(topic, startTime, endTime, participantEmails, availability, false));
        }
        try {
            MeetingResponse meeting = meetingService.createMeeting(new CreateMeetingRequest(
                    topic,
                    description,
                    Instant.parse(startTime),
                    Instant.parse(endTime),
                    participantEmails.stream().map(email -> new ParticipantDto(email, null)).toList(),
                    createdBy));
            return toJson(meeting);
        } catch (SchedulingConflictException ex) {
            return toJson(new ToolSchedulePreview(topic, startTime, endTime, participantEmails,
                    new AvailabilityResponse(false, ex.getConflicts()), true));
        }
    }

    @Tool(description = "Cancel a meeting by UUID. Requires confirmed=true.")
    public String cancelMeeting(
            @ToolParam(description = "Meeting UUID") String meetingId,
            @ToolParam(description = "Must be true to cancel") boolean confirmed) {
        if (!confirmed) {
            return toJson(new ToolActionPreview("cancel", meetingId, false));
        }
        MeetingResponse meeting = meetingService.cancelMeeting(UUID.fromString(meetingId));
        return toJson(meeting);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize tool response", ex);
        }
    }

    private record ToolSchedulePreview(
            String topic,
            String startTime,
            String endTime,
            List<String> participants,
            AvailabilityResponse availability,
            boolean attemptedSchedule
    ) {
    }

    private record ToolActionPreview(String action, String meetingId, boolean confirmed) {
    }
}
