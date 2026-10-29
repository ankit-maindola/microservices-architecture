package com.example.meetingscheduler.ai;

import com.example.meetingscheduler.dto.ParticipantDto;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class MeetingIntentValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public ResolvedMeetingIntent resolve(MeetingIntent intent, ZoneId defaultZoneId) {
        System.out.println("ResolvedMeetingIntent start");
        List<String> missing = new ArrayList<>();
        if (intent.getIntentType() == null) {
            missing.add("intentType");
        }

        ZoneId zoneId = resolveZoneId(intent.getTimeZone(), defaultZoneId, missing);
        Instant start = parseInstant(intent.getStartTime(), zoneId);
        Instant end = parseInstant(intent.getEndTime(), zoneId);
        Integer durationMinutes = intent.getDurationMinutes();

        if (start == null && requiresTime(intent.getIntentType())) {
            missing.add("startTime");
        }

        if (requiresTime(intent.getIntentType())) {
            if (start != null && end != null && durationMinutes != null) {
                long computed = Duration.between(start, end).toMinutes();
                if (computed != durationMinutes) {
                    return ResolvedMeetingIntent.clarification(
                            List.of("durationMinutes", "endTime"),
                            "The duration and end time you provided do not match. Please clarify the meeting length or end time.");
                }
            } else if (start != null && end == null && durationMinutes != null) {
                end = start.plus(Duration.ofMinutes(durationMinutes));
            } else if (start != null && end != null && durationMinutes == null) {
                durationMinutes = (int) Duration.between(start, end).toMinutes();
            } else if (start != null && end == null && durationMinutes == null) {
                missing.add("endTime");
                missing.add("durationMinutes");
            }
        }

        List<ParticipantDto> participants = normalizeParticipants(intent.getParticipants(), missing, intent.getIntentType());

        if (intent.getIntentType() == IntentType.SCHEDULE || intent.getIntentType() == IntentType.UPDATE_MEETING) {
            if (intent.getTopic() == null || intent.getTopic().isBlank()) {
                missing.add("topic");
            }
        }

        if (intent.getIntentType() == IntentType.SCHEDULE && participants.isEmpty()) {
            missing.add("participants");
        }

        if (intent.getIntentType() == IntentType.CHECK_AVAILABILITY && participants.isEmpty()) {
            missing.add("participants");
        }

        if (!intent.getMissingFields().isEmpty()) {
            missing.addAll(intent.getMissingFields());
        }

        if (intent.getIntentType() == IntentType.CLARIFICATION_REQUIRED || !missing.isEmpty()) {
            String question = intent.getClarificationQuestion();
            if (question == null || question.isBlank()) {
                question = buildClarificationQuestion(missing);
            }
            return ResolvedMeetingIntent.clarification(distinct(missing), question);
        }

        Instant listFrom = parseInstant(intent.getListFrom(), zoneId);
        Instant listTo = parseInstant(intent.getListTo(), zoneId);
        System.out.println("ResolvedMeetingIntent end");
        return ResolvedMeetingIntent.resolved(
                intent.getIntentType(),
                intent.getTopic(),
                intent.getDescription(),
                start,
                end,
                durationMinutes,
                participants,
                parseMeetingId(intent.getMeetingId()),
                zoneId,
                listFrom,
                listTo,
                normalizeEmail(intent.getListParticipantEmail()));

    }

    private boolean requiresTime(IntentType intentType) {
        return intentType == IntentType.SCHEDULE
                || intentType == IntentType.CHECK_AVAILABILITY
                || intentType == IntentType.UPDATE_MEETING;
    }

    private ZoneId resolveZoneId(String timeZone, ZoneId defaultZoneId, List<String> missing) {
        if (timeZone == null || timeZone.isBlank()) {
            return defaultZoneId;
        }
        try {
            return ZoneId.of(timeZone);
        } catch (Exception ex) {
            missing.add("timeZone");
            return defaultZoneId;
        }
    }

    private Instant parseInstant(String value, ZoneId zoneId) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return ZonedDateTime.parse(value).toInstant();
            } catch (DateTimeParseException ignoredLocal) {
                try {
                    return java.time.LocalDateTime.parse(value).atZone(zoneId).toInstant();
                } catch (DateTimeParseException ex) {
                    return null;
                }
            }
        }
    }

    private List<ParticipantDto> normalizeParticipants(List<String> raw, List<String> missing, IntentType intentType) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        Set<String> emails = new LinkedHashSet<>();
        List<ParticipantDto> participants = new ArrayList<>();
        for (String entry : raw) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String trimmed = entry.trim();
            if (EMAIL_PATTERN.matcher(trimmed.toLowerCase(Locale.ROOT)).matches()) {
                String email = trimmed.toLowerCase(Locale.ROOT);
                if (emails.add(email)) {
                    participants.add(new ParticipantDto(email, null));
                }
            } else if (intentType == IntentType.SCHEDULE || intentType == IntentType.CHECK_AVAILABILITY) {
                missing.add("participants");
            }
        }
        return participants;
    }

    private UUIDOptional parseMeetingId(String meetingId) {
        if (meetingId == null || meetingId.isBlank()) {
            return UUIDOptional.empty();
        }
        try {
            return UUIDOptional.of(java.util.UUID.fromString(meetingId));
        } catch (IllegalArgumentException ex) {
            return UUIDOptional.invalidValue();
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> distinct(List<String> values) {
        return values.stream().distinct().toList();
    }

    private String buildClarificationQuestion(List<String> missing) {
        if (missing.contains("startTime") || missing.contains("durationMinutes") || missing.contains("endTime")) {
            return "When should the meeting take place? Please provide a start time and either an end time or duration.";
        }
        if (missing.contains("participants")) {
            return "Which participants should attend? Please provide valid email addresses.";
        }
        if (missing.contains("topic")) {
            return "What is the meeting topic?";
        }
        return "Please provide the missing details so I can complete your request.";
    }

    public record ResolvedMeetingIntent(
            boolean clarificationRequired,
            String clarificationQuestion,
            List<String> missingFields,
            IntentType intentType,
            String topic,
            String description,
            Instant startTime,
            Instant endTime,
            Integer durationMinutes,
            List<ParticipantDto> participants,
            Optional<java.util.UUID> meetingId,
            boolean invalidMeetingId,
            ZoneId timeZone,
            Instant listFrom,
            Instant listTo,
            String listParticipantEmail
    ) {
        public static ResolvedMeetingIntent clarification(List<String> missingFields, String question) {
            return new ResolvedMeetingIntent(
                    true, question, missingFields, null, null, null, null, null, null,
                    List.of(), Optional.empty(), false, null, null, null, null);
        }

        public static ResolvedMeetingIntent resolved(
                IntentType intentType,
                String topic,
                String description,
                Instant startTime,
                Instant endTime,
                Integer durationMinutes,
                List<ParticipantDto> participants,
                UUIDOptional meetingId,
                ZoneId timeZone,
                Instant listFrom,
                Instant listTo,
                String listParticipantEmail) {
            return new ResolvedMeetingIntent(
                    false,
                    null,
                    List.of(),
                    intentType,
                    topic,
                    description,
                    startTime,
                    endTime,
                    durationMinutes,
                    participants,
                    meetingId.value(),
                    meetingId.invalidId(),
                    timeZone,
                    listFrom,
                    listTo,
                    listParticipantEmail);
        }
    }

    private record UUIDOptional(Optional<java.util.UUID> value, boolean invalidId) {
        static UUIDOptional empty() {
            return new UUIDOptional(Optional.empty(), false);
        }

        static UUIDOptional of(java.util.UUID id) {
            return new UUIDOptional(Optional.of(id), false);
        }

        static UUIDOptional invalidValue() {
            return new UUIDOptional(Optional.empty(), true);
        }
    }
}
