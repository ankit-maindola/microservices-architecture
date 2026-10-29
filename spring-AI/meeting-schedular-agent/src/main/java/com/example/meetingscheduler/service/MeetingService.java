package com.example.meetingscheduler.service;

import com.example.meetingscheduler.dto.AvailabilityRequest;
import com.example.meetingscheduler.dto.AvailabilityResponse;
import com.example.meetingscheduler.dto.CreateMeetingRequest;
import com.example.meetingscheduler.dto.MeetingResponse;
import com.example.meetingscheduler.dto.ParticipantDto;
import com.example.meetingscheduler.dto.UpdateMeetingRequest;
import com.example.meetingscheduler.entity.Meeting;
import com.example.meetingscheduler.entity.MeetingParticipant;
import com.example.meetingscheduler.entity.MeetingStatus;
import com.example.meetingscheduler.exception.MeetingNotFoundException;
import com.example.meetingscheduler.exception.SchedulingConflictException;
import com.example.meetingscheduler.mapper.MeetingMapper;
import com.example.meetingscheduler.repository.MeetingRepository;
import com.example.meetingscheduler.validation.MeetingTimeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingConflictService meetingConflictService;
    private final MeetingMapper meetingMapper;
    private final MeetingTimeValidator meetingTimeValidator;

    public MeetingService(
            MeetingRepository meetingRepository,
            MeetingConflictService meetingConflictService,
            MeetingMapper meetingMapper,
            MeetingTimeValidator meetingTimeValidator) {
        this.meetingRepository = meetingRepository;
        this.meetingConflictService = meetingConflictService;
        this.meetingMapper = meetingMapper;
        this.meetingTimeValidator = meetingTimeValidator;
    }

    @Transactional
    public MeetingResponse createMeeting(CreateMeetingRequest request) {
        meetingTimeValidator.validate(request.startTime(), request.endTime());
        List<String> emails = request.participants().stream().map(ParticipantDto::email).toList();

        meetingConflictService.lockParticipantsForScheduling(emails, request.startTime(), request.endTime());
        var conflicts = meetingConflictService.findConflicts(emails, request.startTime(), request.endTime(), null);
        if (!conflicts.isEmpty()) {
            throw new SchedulingConflictException(conflicts);
        }

        Meeting meeting = new Meeting(
                request.topic(),
                request.description(),
                request.startTime(),
                request.endTime(),
                request.createdBy());
        meeting.replaceParticipants(toParticipants(request.participants()));
        return meetingMapper.toResponse(meetingRepository.save(meeting));
    }

    @Transactional(readOnly = true)
    public MeetingResponse getMeeting(UUID id) {
        return meetingMapper.toResponse(findMeetingOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> listMeetings(Instant from, Instant to) {
        return meetingMapper.toResponses(meetingRepository.findByStartTimeBetween(from, to));
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> listMeetingsForParticipant(String email, Instant from, Instant to) {
        String normalized = email.trim().toLowerCase();
        return meetingMapper.toResponses(
                meetingRepository.findByParticipantEmailAndStartTimeBetween(normalized, from, to));
    }

    @Transactional
    public MeetingResponse updateMeeting(UUID id, UpdateMeetingRequest request) {
        meetingTimeValidator.validate(request.startTime(), request.endTime());
        Meeting meeting = findMeetingOrThrow(id);
        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot update a cancelled meeting.");
        }

        List<String> emails = request.participants().stream().map(ParticipantDto::email).toList();
        meetingConflictService.lockParticipantsForScheduling(emails, request.startTime(), request.endTime());
        var conflicts = meetingConflictService.findConflicts(emails, request.startTime(), request.endTime(), id);
        if (!conflicts.isEmpty()) {
            throw new SchedulingConflictException(conflicts);
        }

        meeting.setTopic(request.topic());
        meeting.setDescription(request.description());
        meeting.setStartTime(request.startTime());
        meeting.setEndTime(request.endTime());
        meeting.replaceParticipants(toParticipants(request.participants()));
        return meetingMapper.toResponse(meetingRepository.save(meeting));
    }

    @Transactional
    public MeetingResponse cancelMeeting(UUID id) {
        Meeting meeting = findMeetingOrThrow(id);
        meeting.setStatus(MeetingStatus.CANCELLED);
        return meetingMapper.toResponse(meetingRepository.save(meeting));
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(AvailabilityRequest request) {
        meetingTimeValidator.validate(request.startTime(), request.endTime());
        return meetingConflictService.checkAvailability(
                request.participantEmails(), request.startTime(), request.endTime());
    }

    private Meeting findMeetingOrThrow(UUID id) {
        return meetingRepository.findByIdWithParticipants(id).orElseThrow(() -> new MeetingNotFoundException(id));
    }

    private List<MeetingParticipant> toParticipants(List<ParticipantDto> participants) {
        return participants.stream().map(meetingMapper::toEntity).toList();
    }
}
