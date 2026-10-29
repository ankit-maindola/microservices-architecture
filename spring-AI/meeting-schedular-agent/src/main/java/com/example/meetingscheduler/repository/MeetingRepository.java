package com.example.meetingscheduler.repository;

import com.example.meetingscheduler.entity.Meeting;
import com.example.meetingscheduler.entity.MeetingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    @Query("""
            SELECT m FROM Meeting m
            LEFT JOIN FETCH m.participants
            WHERE m.id = :id
            """)
    java.util.Optional<Meeting> findByIdWithParticipants(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT m FROM Meeting m
            JOIN m.participants p
            WHERE m.status = :status
              AND p.email IN :emails
              AND m.startTime < :endTime
              AND m.endTime > :startTime
            ORDER BY m.startTime
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Meeting> findOverlappingScheduledForParticipantsForUpdate(
            @Param("emails") Collection<String> emails,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("status") MeetingStatus status);

    @Query("""
            SELECT DISTINCT m FROM Meeting m
            JOIN FETCH m.participants p
            WHERE m.status = :status
              AND p.email IN :emails
              AND m.startTime < :endTime
              AND m.endTime > :startTime
              AND (:excludeMeetingId IS NULL OR m.id <> :excludeMeetingId)
            ORDER BY m.startTime
            """)
    List<Meeting> findOverlappingScheduledForParticipants(
            @Param("emails") Collection<String> emails,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("status") MeetingStatus status,
            @Param("excludeMeetingId") UUID excludeMeetingId);

    @Query("""
            SELECT DISTINCT m FROM Meeting m
            JOIN FETCH m.participants
            WHERE m.startTime >= :from AND m.startTime < :to
            ORDER BY m.startTime
            """)
    List<Meeting> findByStartTimeBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT DISTINCT m FROM Meeting m
            JOIN FETCH m.participants p
            WHERE p.email = :email
              AND m.startTime >= :from
              AND m.startTime < :to
            ORDER BY m.startTime
            """)
    List<Meeting> findByParticipantEmailAndStartTimeBetween(
            @Param("email") String email,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
