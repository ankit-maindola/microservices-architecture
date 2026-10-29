package com.example.meetingscheduler.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingIntentValidatorTest {

    private MeetingIntentValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MeetingIntentValidator();
    }

    @Test
    void calculatesEndTimeFromDuration() {
        MeetingIntent intent = new MeetingIntent();
        intent.setIntentType(IntentType.SCHEDULE);
        intent.setTopic("Sprint Planning");
        intent.setStartTime("2026-10-29T14:00:00Z");
        intent.setDurationMinutes(30);
        intent.setParticipants(List.of("alice@example.com"));

        var resolved = validator.resolve(intent, ZoneId.of("UTC"));

        assertThat(resolved.clarificationRequired()).isFalse();
        assertThat(resolved.endTime()).isEqualTo(java.time.Instant.parse("2026-10-29T14:30:00Z"));
    }

    @Test
    void requiresClarificationWhenDurationAndEndTimeDisagree() {
        MeetingIntent intent = new MeetingIntent();
        intent.setIntentType(IntentType.SCHEDULE);
        intent.setTopic("Review");
        intent.setStartTime("2026-10-29T14:00:00Z");
        intent.setEndTime("2026-10-29T15:00:00Z");
        intent.setDurationMinutes(30);
        intent.setParticipants(List.of("alice@example.com"));

        var resolved = validator.resolve(intent, ZoneId.of("UTC"));

        assertThat(resolved.clarificationRequired()).isTrue();
        assertThat(resolved.missingFields()).contains("durationMinutes", "endTime");
    }

    @Test
    void doesNotInventEmailsForNamesOnly() {
        MeetingIntent intent = new MeetingIntent();
        intent.setIntentType(IntentType.SCHEDULE);
        intent.setTopic("1:1");
        intent.setStartTime("2026-10-29T14:00:00Z");
        intent.setEndTime("2026-10-29T15:00:00Z");
        intent.setParticipants(List.of("Alice"));

        var resolved = validator.resolve(intent, ZoneId.of("UTC"));

        assertThat(resolved.clarificationRequired()).isTrue();
        assertThat(resolved.missingFields()).contains("participants");
    }
}
