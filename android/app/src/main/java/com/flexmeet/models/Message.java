package com.flexmeet.models;

import java.io.Serializable;

/**
 * Message Model
 * Represents a chat message in the application
 */
public class Message implements Serializable {

    private String messageId;
    private String senderId;
    private String senderName;
    private String senderPhotoUrl;
    private String meetingId;
    private String text;
    private String imageUrl;
    private long timestamp;
    private boolean isRead;
    private String messageType; // text, image, file, location

    // Default Constructor
    public Message() {
    }

    // Parameterized Constructor
    public Message(String messageId, String senderId, String senderName, String meetingId, String text) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.meetingId = meetingId;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
        this.messageType = "text";
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderPhotoUrl() {
        return senderPhotoUrl;
    }

    public void setSenderPhotoUrl(String senderPhotoUrl) {
        this.senderPhotoUrl = senderPhotoUrl;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
}
