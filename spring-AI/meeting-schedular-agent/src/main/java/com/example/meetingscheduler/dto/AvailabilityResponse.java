package com.example.meetingscheduler.dto;

import java.util.List;

public record AvailabilityResponse(
        boolean available,
        List<ConflictDetail> conflicts
) {
}
