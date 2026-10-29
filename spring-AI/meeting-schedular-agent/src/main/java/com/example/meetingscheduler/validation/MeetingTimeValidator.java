package com.example.meetingscheduler.validation;

import com.example.meetingscheduler.exception.InvalidMeetingTimeException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class MeetingTimeValidator {

    private final Clock clock;

    public MeetingTimeValidator(Clock clock) {
        this.clock = clock;
    }

    public void validate(Instant startTime, Instant endTime) {
        if (startTime == null || endTime == null) {
            throw new InvalidMeetingTimeException("Start time and end time are required.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new InvalidMeetingTimeException("End time must be after start time.");
        }
        if (startTime.isBefore(clock.instant())) {
            throw new InvalidMeetingTimeException("Start time must not be in the past.");
        }
    }
}
