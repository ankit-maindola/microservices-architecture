package com.example.meetingscheduler.exception;

import com.example.meetingscheduler.dto.ConflictDetail;

import java.util.List;

public class SchedulingConflictException extends RuntimeException {

    private final List<ConflictDetail> conflicts;

    public SchedulingConflictException(List<ConflictDetail> conflicts) {
        super("Scheduling conflict detected for one or more participants.");
        this.conflicts = List.copyOf(conflicts);
    }

    public List<ConflictDetail> getConflicts() {
        return conflicts;
    }
}
