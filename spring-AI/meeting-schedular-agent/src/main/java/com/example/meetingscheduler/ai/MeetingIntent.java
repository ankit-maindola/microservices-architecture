package com.example.meetingscheduler.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MeetingIntent {

    private IntentType intentType;
    private String topic;
    private String description;
    /** ISO-8601 instant or local date-time with offset, interpreted in timeZone when offset absent. */
    private String startTime;
    private String endTime;
    private Integer durationMinutes;
    private List<String> participants = new ArrayList<>();
    private String meetingId;
    private String timeZone;
    private List<String> missingFields = new ArrayList<>();
    private String clarificationQuestion;
    /** Optional ISO-8601 date range for listing meetings. */
    private String listFrom;
    private String listTo;
    /** Optional participant filter for listing. */
    private String listParticipantEmail;

    public IntentType getIntentType() {
        return intentType;
    }

    public void setIntentType(IntentType intentType) {
        this.intentType = intentType;
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

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants != null ? participants : new ArrayList<>();
    }

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields != null ? missingFields : new ArrayList<>();
    }

    public String getClarificationQuestion() {
        return clarificationQuestion;
    }

    public void setClarificationQuestion(String clarificationQuestion) {
        this.clarificationQuestion = clarificationQuestion;
    }

    public String getListFrom() {
        return listFrom;
    }

    public void setListFrom(String listFrom) {
        this.listFrom = listFrom;
    }

    public String getListTo() {
        return listTo;
    }

    public void setListTo(String listTo) {
        this.listTo = listTo;
    }

    public String getListParticipantEmail() {
        return listParticipantEmail;
    }

    public void setListParticipantEmail(String listParticipantEmail) {
        this.listParticipantEmail = listParticipantEmail;
    }
}
