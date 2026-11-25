package com.flexmeet.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "participants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "participant_name")
    private String participantName;

    @Column(name = "participant_email")
    private String participantEmail;

    @Column(name = "camera_enabled")
    private Boolean cameraEnabled;

    @Column(name = "microphone_enabled")
    private Boolean microphoneEnabled;

    @Column(name = "screen_sharing_enabled")
    private Boolean screenSharingEnabled;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "is_host")
    private Boolean isHost;

    @Column(name = "is_muted")
    private Boolean isMuted;

    @Column(name = "participant_uuid")
    private String participantUUID;

    @Column(name = "firebase_connection_ref")
    private String firebaseConnectionRef;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
        cameraEnabled = true;
        microphoneEnabled = true;
        screenSharingEnabled = false;
        isMuted = false;
    }
}
