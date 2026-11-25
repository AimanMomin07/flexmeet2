package com.flexmeet.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Meeting implements Serializable {
    private String meetingId;
    private String title;
    private String hostId;
    private long scheduledTime;
    private long duration;
    private String status; // active, scheduled, completed
    private List<String> participants;

    public Meeting() {
        this.participants = new ArrayList<>();
    }

    public Meeting(String meetingId, String title, String hostId, long scheduledTime) {
        this.meetingId = meetingId;
        this.title = title;
        this.hostId = hostId;
        this.scheduledTime = scheduledTime;
        this.status = "scheduled";
        this.participants = new ArrayList<>();
        this.participants.add(hostId);
    }

    public String getMeetingId() { return meetingId; }
    public void setMeetingId(String meetingId) { this.meetingId = meetingId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }
    public long getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(long scheduledTime) { this.scheduledTime = scheduledTime; }
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }
}
