package com.example.meetingscheduler.service;

import com.example.meetingscheduler.config.TestClockConfig;
import com.example.meetingscheduler.dto.CreateMeetingRequest;
import com.example.meetingscheduler.dto.ParticipantDto;
import com.example.meetingscheduler.dto.UpdateMeetingRequest;
import com.example.meetingscheduler.exception.InvalidMeetingTimeException;
import com.example.meetingscheduler.exception.SchedulingConflictException;
import com.example.meetingscheduler.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({TestClockConfig.class})
@Transactional
class MeetingServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MeetingService meetingService;

    @Test
    void createsMeetingSuccessfully() {
        var response = meetingService.createMeeting(new CreateMeetingRequest(
                "Project Review",
                "Q4 review",
                Instant.parse("2026-11-05T14:00:00Z"),
                Instant.parse("2026-11-05T15:00:00Z"),
                List.of(
                        new ParticipantDto("alice@example.com", "Alice"),
                        new ParticipantDto("bob@example.com", "Bob")),
                "organizer"));

        assertThat(response.id()).isNotNull();
        assertThat(response.participants()).hasSize(2);
    }

    @Test
    void rejectsInvalidTimes() {
        assertThatThrownBy(() -> meetingService.createMeeting(new CreateMeetingRequest(
                "Bad times",
                null,
                Instant.parse("2026-11-05T15:00:00Z"),
                Instant.parse("2026-11-05T14:00:00Z"),
                List.of(new ParticipantDto("alice@example.com", null)),
                null)))
                .isInstanceOf(InvalidMeetingTimeException.class);
    }

    @Test
    void updateRechecksConflicts() {
        var first = meetingService.createMeeting(new CreateMeetingRequest(
                "A",
                null,
                Instant.parse("2026-11-06T10:00:00Z"),
                Instant.parse("2026-11-06T11:00:00Z"),
                List.of(new ParticipantDto("alice@example.com", null)),
                null));
        meetingService.createMeeting(new CreateMeetingRequest(
                "B",
                null,
                Instant.parse("2026-11-06T12:00:00Z"),
                Instant.parse("2026-11-06T13:00:00Z"),
                List.of(new ParticipantDto("alice@example.com", null)),
                null));

        assertThatThrownBy(() -> meetingService.updateMeeting(first.id(), new UpdateMeetingRequest(
                "A moved",
                null,
                Instant.parse("2026-11-06T12:30:00Z"),
                Instant.parse("2026-11-06T13:30:00Z"),
                List.of(new ParticipantDto("alice@example.com", null)))))
                .isInstanceOf(SchedulingConflictException.class);
    }
}
