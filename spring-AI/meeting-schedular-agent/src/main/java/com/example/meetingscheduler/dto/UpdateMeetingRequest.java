package com.example.meetingscheduler.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record UpdateMeetingRequest(
        @NotBlank @Size(max = 500) String topic,
        @Size(max = 2000) String description,
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        @NotEmpty List<@Valid ParticipantDto> participants
) {
}
