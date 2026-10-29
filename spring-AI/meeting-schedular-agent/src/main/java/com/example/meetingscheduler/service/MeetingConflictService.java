package com.example.meetingscheduler.service;

import com.example.meetingscheduler.dto.AvailabilityResponse;
import com.example.meetingscheduler.dto.ConflictDetail;
import com.example.meetingscheduler.entity.Meeting;
import com.example.meetingscheduler.entity.MeetingStatus;
import com.example.meetingscheduler.repository.MeetingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MeetingConflictService {

    private final MeetingRepository meetingRepository;

    public MeetingConflictService(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(Collection<String> participantEmails, Instant startTime, Instant endTime) {
        List<ConflictDetail> conflicts = findConflicts(participantEmails, startTime, endTime, null);
        return new AvailabilityResponse(conflicts.isEmpty(), conflicts);
    }

    @Transactional(readOnly = true)
    public List<ConflictDetail> findConflicts(
            Collection<String> participantEmails,
            Instant startTime,
            Instant endTime,
            UUID excludeMeetingId) {
        if (participantEmails == null || participantEmails.isEmpty()) {
            return List.of();
        }
        Set<String> normalizedEmails = participantEmails.stream()
                .map(email -> email.trim().toLowerCase())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Meeting> overlapping = meetingRepository.findOverlappingScheduledForParticipants(
                normalizedEmails,
                startTime,
                endTime,
                MeetingStatus.SCHEDULED,
                excludeMeetingId);

        Map<UUID, ConflictDetailBuilder> builders = new LinkedHashMap<>();
        for (Meeting meeting : overlapping) {
            ConflictDetailBuilder builder = builders.computeIfAbsent(
                    meeting.getId(),
                    id -> new ConflictDetailBuilder(meeting));
            for (var participant : meeting.getParticipants()) {
                if (normalizedEmails.contains(participant.getEmail())) {
                    builder.addEmail(participant.getEmail());
                }
            }
        }
        return builders.values().stream()
                .map(ConflictDetailBuilder::build)
                .filter(detail -> !detail.conflictingParticipantEmails().isEmpty())
                .toList();
    }

    @Transactional
    public void lockParticipantsForScheduling(Collection<String> participantEmails, Instant startTime, Instant endTime) {
        Set<String> normalizedEmails = participantEmails.stream()
                .map(email -> email.trim().toLowerCase())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        meetingRepository.findOverlappingScheduledForParticipantsForUpdate(
                normalizedEmails, startTime, endTime, MeetingStatus.SCHEDULED);
    }

    private static final class ConflictDetailBuilder {
        private final UUID meetingId;
        private final String topic;
        private final Instant startTime;
        private final Instant endTime;
        private final List<String> emails = new ArrayList<>();

        ConflictDetailBuilder(Meeting meeting) {
            this.meetingId = meeting.getId();
            this.topic = meeting.getTopic();
            this.startTime = meeting.getStartTime();
            this.endTime = meeting.getEndTime();
        }

        void addEmail(String email) {
            if (!emails.contains(email)) {
                emails.add(email);
            }
        }

        ConflictDetail build() {
            return new ConflictDetail(meetingId, topic, startTime, endTime, List.copyOf(emails));
        }
    }
}
