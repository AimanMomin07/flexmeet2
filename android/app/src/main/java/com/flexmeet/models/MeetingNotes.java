package com.flexmeet.models;

import java.io.Serializable;

/**
 * MeetingNotes Model
 * Represents notes taken during a meeting
 */
public class MeetingNotes implements Serializable {

    private String noteId;
    private String meetingId;
    private String userId;
    private String content;
    private long createdAt;
    private long updatedAt;
    private String lastEditedBy;

    // Default Constructor
    public MeetingNotes() {
    }

    // Parameterized Constructor
    public MeetingNotes(String noteId, String meetingId, String userId, String content) {
        this.noteId = noteId;
        this.meetingId = meetingId;
        this.userId = userId;
        this.content = content;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getLastEditedBy() {
        return lastEditedBy;
    }

    public void setLastEditedBy(String lastEditedBy) {
        this.lastEditedBy = lastEditedBy;
    }
}
