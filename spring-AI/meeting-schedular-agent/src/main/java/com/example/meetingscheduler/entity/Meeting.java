package com.example.meetingscheduler.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "meetings")
public class Meeting {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String topic;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingStatus status;

    @Size(max = 255)
    @Column(name = "created_by", length = 255)
    private String createdBy;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MeetingParticipant> participants = new ArrayList<>();

    protected Meeting() {
    }

    public Meeting(String topic, String description, Instant startTime, Instant endTime, String createdBy) {
        this.id = UUID.randomUUID();
        this.topic = topic;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = MeetingStatus.SCHEDULED;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void replaceParticipants(List<MeetingParticipant> newParticipants) {
        participants.clear();
        Set<String> seen = new LinkedHashSet<>();
        for (MeetingParticipant participant : newParticipants) {
            String normalized = participant.getEmail().trim().toLowerCase();
            if (!seen.add(normalized)) {
                throw new IllegalArgumentException("Duplicate participant in meeting: " + normalized);
            }
            participant.setMeeting(this);
            participants.add(participant);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public MeetingStatus getStatus() {
        return status;
    }

    public void setStatus(MeetingStatus status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<MeetingParticipant> getParticipants() {
        return List.copyOf(participants);
    }
}
