package com.example.meetingscheduler.dto;

import com.example.meetingscheduler.entity.MeetingStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MeetingResponse(
        UUID id,
        String topic,
        String description,
        Instant startTime,
        Instant endTime,
        MeetingStatus status,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        List<ParticipantDto> participants
) {
}
