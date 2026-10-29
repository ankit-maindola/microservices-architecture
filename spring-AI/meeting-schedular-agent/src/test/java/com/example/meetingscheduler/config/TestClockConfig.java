package com.example.meetingscheduler.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@TestConfiguration
public class TestClockConfig {

    public static final Instant FIXED_NOW = Instant.parse("2026-10-28T10:00:00Z");

    @Bean
    @Primary
    Clock testClock() {
        return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    }
}
