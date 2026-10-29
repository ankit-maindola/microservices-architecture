package com.example.meetingscheduler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NaturalLanguageRequest(
        @NotBlank @Size(max = 4000) String prompt,
        /** IANA zone id, e.g. Asia/Kolkata */
        @Size(max = 64) String timeZone,
        @Size(max = 255) String createdBy,
        boolean confirmSchedule
) {
}
