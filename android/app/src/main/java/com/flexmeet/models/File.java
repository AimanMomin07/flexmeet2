package com.flexmeet.models;

import java.io.Serializable;

/**
 * File Model
 * Represents a file shared during a meeting
 */
public class File implements Serializable {

    private String fileId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private long fileSize;
    private String uploadedBy;
    private String uploaderName;
    private String meetingId;
    private long uploadTime;
    private String storagePath; // Path in Firebase Storage

    // Default Constructor
    public File() {
    }

    // Parameterized Constructor
    public File(String fileId, String fileName, String fileType, long fileSize, String uploadedBy) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
        this.uploadTime = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    public long getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(long uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    // Utility method to get file size in KB/MB
    public String getFileSizeInKB() {
        long sizeInKB = fileSize / 1024;
        if (sizeInKB > 1024) {
            return (sizeInKB / 1024) + " MB";
        }
        return sizeInKB + " KB";
    }
}
