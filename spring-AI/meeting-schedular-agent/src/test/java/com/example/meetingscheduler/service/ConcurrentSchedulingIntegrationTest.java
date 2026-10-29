package com.example.meetingscheduler.service;

import com.example.meetingscheduler.config.TestClockConfig;
import com.example.meetingscheduler.dto.CreateMeetingRequest;
import com.example.meetingscheduler.dto.ParticipantDto;
import com.example.meetingscheduler.exception.SchedulingConflictException;
import com.example.meetingscheduler.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Import({TestClockConfig.class})
class ConcurrentSchedulingIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MeetingService meetingService;

    @Test
    void onlyOneConcurrentBookingSucceedsForSameParticipant() throws InterruptedException {
        Instant start = Instant.parse("2026-11-07T14:00:00Z");
        Instant end = Instant.parse("2026-11-07T15:00:00Z");
        CreateMeetingRequest request = new CreateMeetingRequest(
                "Concurrent",
                null,
                start,
                end,
                List.of(new ParticipantDto("alice@example.com", null)),
                "tester");

        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    startLatch.await();
                    meetingService.createMeeting(request);
                    success.incrementAndGet();
                } catch (SchedulingConflictException ex) {
                    conflicts.incrementAndGet();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await();
        startLatch.countDown();
        executor.shutdown();

        assertThat(success.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(threads - 1);
    }
}
