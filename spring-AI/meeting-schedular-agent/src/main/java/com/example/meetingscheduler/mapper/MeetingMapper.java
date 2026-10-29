package com.example.meetingscheduler.mapper;

import com.example.meetingscheduler.dto.MeetingResponse;
import com.example.meetingscheduler.dto.ParticipantDto;
import com.example.meetingscheduler.entity.Meeting;
import com.example.meetingscheduler.entity.MeetingParticipant;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MeetingMapper {

    public MeetingResponse toResponse(Meeting meeting) {
        List<ParticipantDto> participants = meeting.getParticipants().stream()
                .map(this::toParticipantDto)
                .toList();
        return new MeetingResponse(
                meeting.getId(),
                meeting.getTopic(),
                meeting.getDescription(),
                meeting.getStartTime(),
                meeting.getEndTime(),
                meeting.getStatus(),
                meeting.getCreatedBy(),
                meeting.getCreatedAt(),
                meeting.getUpdatedAt(),
                participants);
    }

    public List<MeetingResponse> toResponses(List<Meeting> meetings) {
        return meetings.stream().map(this::toResponse).toList();
    }

    public MeetingParticipant toEntity(ParticipantDto dto) {
        return new MeetingParticipant(dto.email(), dto.displayName());
    }

    private ParticipantDto toParticipantDto(MeetingParticipant participant) {
        return new ParticipantDto(participant.getEmail(), participant.getDisplayName());
    }
}
