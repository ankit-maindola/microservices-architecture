package com.example.meetingscheduler.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record AvailabilityRequest(
        @NotEmpty List<@Email String> participantEmails,
        @NotNull Instant startTime,
        @NotNull Instant endTime
) {
    public AvailabilityRequest {
        participantEmails = participantEmails.stream()
                .map(email -> email.trim().toLowerCase())
                .distinct()
                .toList();
    }
}
