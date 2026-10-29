package com.example.meetingscheduler.service;

import com.example.meetingscheduler.config.TestClockConfig;
import com.example.meetingscheduler.dto.CreateMeetingRequest;
import com.example.meetingscheduler.dto.ParticipantDto;
import com.example.meetingscheduler.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import({TestClockConfig.class})
@Transactional
class MeetingConflictServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private MeetingConflictService meetingConflictService;

    @BeforeEach
    void cleanDatabase() {
        // Schema is created/updated by Hibernate (ddl-auto=update).
    }

    @Test
    void detectsOverlapForSameParticipant() {
        Instant start = Instant.parse("2026-11-01T14:00:00Z");
        Instant end = Instant.parse("2026-11-01T15:00:00Z");
        meetingService.createMeeting(new CreateMeetingRequest(
                "Existing",
                null,
                start,
                end,
                List.of(new ParticipantDto("alice@example.com", "Alice")),
                "tester"));

        var availability = meetingConflictService.checkAvailability(
                List.of("alice@example.com"),
                Instant.parse("2026-11-01T14:30:00Z"),
                Instant.parse("2026-11-01T15:30:00Z"));

        assertThat(availability.available()).isFalse();
        assertThat(availability.conflicts()).hasSize(1);
        assertThat(availability.conflicts().getFirst().conflictingParticipantEmails()).containsExactly("alice@example.com");
    }

    @Test
    void backToBackMeetingsDoNotConflict() {
        Instant firstStart = Instant.parse("2026-11-02T14:00:00Z");
        Instant firstEnd = Instant.parse("2026-11-02T15:00:00Z");
        meetingService.createMeeting(new CreateMeetingRequest(
                "First",
                null,
                firstStart,
                firstEnd,
                List.of(new ParticipantDto("bob@example.com", null)),
                "tester"));

        var availability = meetingConflictService.checkAvailability(
                List.of("bob@example.com"),
                Instant.parse("2026-11-02T15:00:00Z"),
                Instant.parse("2026-11-02T16:00:00Z"));

        assertThat(availability.available()).isTrue();
    }

    @Test
    void cancelledMeetingsDoNotConflict() {
        Instant start = Instant.parse("2026-11-03T10:00:00Z");
        Instant end = Instant.parse("2026-11-03T11:00:00Z");
        var created = meetingService.createMeeting(new CreateMeetingRequest(
                "To cancel",
                null,
                start,
                end,
                List.of(new ParticipantDto("carol@example.com", null)),
                "tester"));
        meetingService.cancelMeeting(created.id());

        var availability = meetingConflictService.checkAvailability(
                List.of("carol@example.com"), start, end);

        assertThat(availability.available()).isTrue();
    }

    @Test
    void differentParticipantsDoNotConflictWithEachOther() {
        meetingService.createMeeting(new CreateMeetingRequest(
                "Alice only",
                null,
                Instant.parse("2026-11-04T09:00:00Z"),
                Instant.parse("2026-11-04T10:00:00Z"),
                List.of(new ParticipantDto("alice@example.com", null)),
                "tester"));

        var availability = meetingConflictService.checkAvailability(
                List.of("bob@example.com"),
                Instant.parse("2026-11-04T09:00:00Z"),
                Instant.parse("2026-11-04T10:00:00Z"));

        assertThat(availability.available()).isTrue();
    }
}
