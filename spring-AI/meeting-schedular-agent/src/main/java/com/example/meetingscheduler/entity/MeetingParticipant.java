package com.example.meetingscheduler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(
        name = "meeting_participants",
        uniqueConstraints = @UniqueConstraint(name = "uq_meeting_participant_email", columnNames = {"meeting_id", "email"})
)
public class MeetingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @NotBlank
    @Email
    @Size(max = 320)
    @Column(nullable = false, length = 320)
    private String email;

    @Size(max = 255)
    @Column(name = "display_name", length = 255)
    private String displayName;

    protected MeetingParticipant() {
    }

    public MeetingParticipant(String email, String displayName) {
        this.email = email.trim().toLowerCase();
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
    }

    public Meeting getMeeting() {
        return meeting;
    }

    void setMeeting(Meeting meeting) {
        this.meeting = meeting;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }
}
