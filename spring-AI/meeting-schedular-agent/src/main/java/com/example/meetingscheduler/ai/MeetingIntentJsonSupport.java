package com.example.meetingscheduler.ai;

import com.fasterxml.jackson.databind.ObjectMapper;

final class MeetingIntentJsonSupport {

    static final String RESPONSE_SHAPE = """
            Return one JSON object only (no markdown). Fields:
            intentType (required): SCHEDULE, CHECK_AVAILABILITY, LIST_MEETINGS, CANCEL_MEETING, UPDATE_MEETING, or CLARIFICATION_REQUIRED;
            topic, description, startTime, endTime, listFrom, listTo, meetingId, timeZone, clarificationQuestion, listParticipantEmail (strings, optional);
            durationMinutes (integer, optional); participants (array of email strings); missingFields (array of strings).
            """;

    private MeetingIntentJsonSupport() {
    }

    static MeetingIntent read(ObjectMapper objectMapper, String rawContent) {
        try {
            return objectMapper.readValue(normalizeJson(rawContent), MeetingIntent.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Gemini returned JSON that could not be parsed as MeetingIntent.", ex);
        }
    }

    static String normalizeJson(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstLine = trimmed.indexOf('\n');
        int closingFence = trimmed.lastIndexOf("```");
        if (firstLine >= 0 && closingFence > firstLine) {
            return trimmed.substring(firstLine + 1, closingFence).trim();
        }
        return trimmed.replace("```json", "").replace("```", "").trim();
    }
}
