package com.example.meetingscheduler.dto;

import com.example.meetingscheduler.ai.IntentType;

import java.util.List;

public record AiMeetingResponse(
        IntentType outcome,
        String message,
        MeetingResponse meeting,
        List<MeetingResponse> meetings,
        AvailabilityResponse availability,
        List<ConflictDetail> conflicts,
        List<String> missingFields,
        String clarificationQuestion
) {
    public static AiMeetingResponse scheduled(MeetingResponse meeting) {
        return new AiMeetingResponse(
                IntentType.SCHEDULE,
                "Meeting scheduled successfully.",
                meeting,
                List.of(),
                null,
                List.of(),
                List.of(),
                null);
    }

    public static AiMeetingResponse availability(AvailabilityResponse availability) {
        String message = availability.available()
                ? "All participants are available for the requested time."
                : "One or more participants have scheduling conflicts.";
        return new AiMeetingResponse(
                IntentType.CHECK_AVAILABILITY,
                message,
                null,
                List.of(),
                availability,
                availability.conflicts(),
                List.of(),
                null);
    }

    public static AiMeetingResponse conflicts(List<ConflictDetail> conflicts) {
        return new AiMeetingResponse(
                IntentType.SCHEDULE,
                "Scheduling conflict detected. Meeting was not created.",
                null,
                List.of(),
                null,
                conflicts,
                List.of(),
                null);
    }

    public static AiMeetingResponse clarification(List<String> missingFields, String question) {
        return new AiMeetingResponse(
                IntentType.CLARIFICATION_REQUIRED,
                question,
                null,
                List.of(),
                null,
                List.of(),
                missingFields,
                question);
    }

    public static AiMeetingResponse cancelled(MeetingResponse meeting) {
        return new AiMeetingResponse(
                IntentType.CANCEL_MEETING,
                "Meeting cancelled.",
                meeting,
                List.of(),
                null,
                List.of(),
                List.of(),
                null);
    }

    public static AiMeetingResponse listed(List<MeetingResponse> meetings) {
        return new AiMeetingResponse(
                IntentType.LIST_MEETINGS,
                "Found " + meetings.size() + " meeting(s).",
                null,
                meetings,
                null,
                List.of(),
                List.of(),
                null);
    }
}
