package com.example.meetingscheduler.controller;

import com.example.meetingscheduler.dto.AvailabilityRequest;
import com.example.meetingscheduler.dto.AvailabilityResponse;
import com.example.meetingscheduler.dto.CreateMeetingRequest;
import com.example.meetingscheduler.dto.MeetingResponse;
import com.example.meetingscheduler.dto.UpdateMeetingRequest;
import com.example.meetingscheduler.service.MeetingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @PostMapping
    public ResponseEntity<MeetingResponse> createMeeting(@Valid @RequestBody CreateMeetingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(meetingService.createMeeting(request));
    }

    @PostMapping("/availability")
    public ResponseEntity<AvailabilityResponse> checkAvailability(@Valid @RequestBody AvailabilityRequest request) {
        AvailabilityResponse response = meetingService.checkAvailability(request);
        HttpStatus status = response.available() ? HttpStatus.OK : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping
    public List<MeetingResponse> listMeetings(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return meetingService.listMeetings(from, to);
    }

    @GetMapping("/participant")
    public List<MeetingResponse> listMeetingsForParticipant(
            @RequestParam String email,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return meetingService.listMeetingsForParticipant(email, from, to);
    }

    @GetMapping(ApiPathPatterns.MEETING_ID)
    public MeetingResponse getMeeting(@PathVariable UUID id) {
        return meetingService.getMeeting(id);
    }

    @PutMapping(ApiPathPatterns.MEETING_ID)
    public MeetingResponse updateMeeting(@PathVariable UUID id, @Valid @RequestBody UpdateMeetingRequest request) {
        return meetingService.updateMeeting(id, request);
    }

    @DeleteMapping(ApiPathPatterns.MEETING_ID)
    public MeetingResponse cancelMeeting(@PathVariable UUID id) {
        return meetingService.cancelMeeting(id);
    }

}
