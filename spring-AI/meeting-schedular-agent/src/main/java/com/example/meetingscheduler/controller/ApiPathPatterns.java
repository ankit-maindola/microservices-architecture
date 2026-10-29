package com.example.meetingscheduler.controller;

final class ApiPathPatterns {

    static final String MEETING_ID =
            "{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}";

    private ApiPathPatterns() {
    }
}
