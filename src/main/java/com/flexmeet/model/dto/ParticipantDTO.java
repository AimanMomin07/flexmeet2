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
public class ParticipantDTO {

    private Long id;
    private String participantName;
    private String participantEmail;
    private Boolean cameraEnabled;
    private Boolean microphoneEnabled;
    private Boolean screenSharingEnabled;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private Boolean isHost;
    private Boolean isMuted;
    private String participantUUID;
}
