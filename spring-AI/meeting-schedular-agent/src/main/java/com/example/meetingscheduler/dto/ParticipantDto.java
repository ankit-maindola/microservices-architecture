package com.example.meetingscheduler.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParticipantDto(
        @NotBlank @Email @Size(max = 320) String email,
        @Size(max = 255) String displayName
) {
    public ParticipantDto {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }
}
