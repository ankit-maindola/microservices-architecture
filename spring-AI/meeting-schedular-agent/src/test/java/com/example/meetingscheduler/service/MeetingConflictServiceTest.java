package com.example.meetingscheduler.service;

import com.example.meetingscheduler.entity.Meeting;
import com.example.meetingscheduler.entity.MeetingParticipant;
import com.example.meetingscheduler.entity.MeetingStatus;
import com.example.meetingscheduler.repository.MeetingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingConflictServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @InjectMocks
    private MeetingConflictService meetingConflictService;

    @Test
    void reportsAllParticipantsWithConflicts() {
        Meeting meeting = new Meeting(
                "Overlap",
                null,
                Instant.parse("2026-11-01T14:00:00Z"),
                Instant.parse("2026-11-01T15:00:00Z"),
                "tester");
        meeting.replaceParticipants(List.of(
                new MeetingParticipant("alice@example.com", null),
                new MeetingParticipant("bob@example.com", null)));

        when(meetingRepository.findOverlappingScheduledForParticipants(
                any(), any(), any(), eq(MeetingStatus.SCHEDULED), eq(null)))
                .thenReturn(List.of(meeting));

        var conflicts = meetingConflictService.findConflicts(
                List.of("alice@example.com", "bob@example.com"),
                Instant.parse("2026-11-01T14:30:00Z"),
                Instant.parse("2026-11-01T15:30:00Z"),
                null);

        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.getFirst().conflictingParticipantEmails())
                .containsExactlyInAnyOrder("alice@example.com", "bob@example.com");
    }
}
