package com.example.meetingscheduler.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConflictDetail(
        UUID meetingId,
        String topic,
        Instant startTime,
        Instant endTime,
        List<String> conflictingParticipantEmails
) {
}
