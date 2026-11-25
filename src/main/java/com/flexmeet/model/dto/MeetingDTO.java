package com.flexmeet.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingDTO {

    private Long id;
    private String meetingCode;
    private String title;
    private String description;
    private UserDTO host;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer maxParticipants;
    private Boolean isActive;
    private Boolean allowRecording;
    private Boolean allowScreenShare;
    private Boolean allowChat;
    private LocalDateTime createdAt;
    private Integer currentParticipants;
}
