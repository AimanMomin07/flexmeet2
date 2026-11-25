package com.flexmeet.service.meeting;

import com.flexmeet.model.entity.Participant;
import com.flexmeet.model.entity.Meeting;
import com.flexmeet.model.entity.User;
import com.flexmeet.model.dto.ParticipantDTO;
import com.flexmeet.model.request.JoinMeetingRequest;
import com.flexmeet.repository.ParticipantRepository;
import com.flexmeet.repository.MeetingRepository;
import com.flexmeet.service.firebase.FirebaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@Transactional
public class ParticipantService {

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private FirebaseService firebaseService;

    /**
     * Add participant to meeting
     */
    public ParticipantDTO joinMeeting(Meeting meeting, User user, JoinMeetingRequest request) {
        try {
            // Check if meeting is active
            if (!meeting.getIsActive()) {
                throw new RuntimeException("Meeting is not active");
            }

            // Check max participants
            Integer currentParticipants = participantRepository.countByMeetingAndLeftAtIsNull(meeting);
            if (currentParticipants >= meeting.getMaxParticipants()) {
                throw new RuntimeException("Meeting is full");
            }

            // Check password
            if (meeting.getPassword() != null && !meeting.getPassword().isEmpty()) {
                if (!meeting.getPassword().equals(request.getPassword())) {
                    throw new RuntimeException("Invalid meeting password");
                }
            }

            String participantUUID = UUID.randomUUID().toString();

            Participant participant = Participant.builder()
                    .meeting(meeting)
                    .user(user)
                    .participantName(request.getParticipantName() != null ? 
                            request.getParticipantName() : (user != null ? user.getFullName() : "Anonymous"))
                    .participantEmail(user != null ? user.getEmail() : request.getParticipantName())
                    .cameraEnabled(request.getCameraEnabled() != null ? request.getCameraEnabled() : true)
                    .microphoneEnabled(request.getMicrophoneEnabled() != null ? request.getMicrophoneEnabled() : true)
                    .screenSharingEnabled(false)
                    .isHost(false)
                    .isMuted(false)
                    .participantUUID(participantUUID)
                    .build();

            participant = participantRepository.save(participant);

            // Store participant in Firebase
            Map<String, Object> participantData = new HashMap<>();
            participantData.put("participantName", participant.getParticipantName());
            participantData.put("participantEmail", participant.getParticipantEmail());
            participantData.put("cameraEnabled", participant.getCameraEnabled());
            participantData.put("microphoneEnabled", participant.getMicrophoneEnabled());
            participantData.put("joinedAt", System.currentTimeMillis());
            participantData.put("uuid", participantUUID);

            firebaseService.storeParticipantData(meeting.getMeetingCode(), participantUUID, participantData)
                    .thenAccept(v -> log.info("Participant stored in Firebase"))
                    .exceptionally(e -> {
                        log.error("Failed to store participant in Firebase: " + e.getMessage());
                        return null;
                    });

            return mapToDTO(participant);

        } catch (Exception e) {
            log.error("Error joining meeting: {}", e.getMessage());
            throw new RuntimeException("Failed to join meeting: " + e.getMessage());
        }
    }

    /**
     * Remove participant from meeting
     */
    public void leaveMeeting(String participantUUID, String meetingCode) {
        try {
            Optional<Participant> participant = participantRepository.findByParticipantUUID(participantUUID);
            if (participant.isPresent()) {
                Participant existingParticipant = participant.get();
                existingParticipant.setLeftAt(LocalDateTime.now());
                participantRepository.save(existingParticipant);

                // Remove from Firebase
                firebaseService.removeParticipant(meetingCode, participantUUID)
                        .thenAccept(v -> log.info("Participant removed from Firebase"))
                        .exceptionally(e -> {
                            log.error("Failed to remove participant from Firebase: " + e.getMessage());
                            return null;
                        });
            }
        } catch (Exception e) {
            log.error("Error leaving meeting: {}", e.getMessage());
        }
    }

    /**
     * Get all participants in a meeting
     */
    public List<ParticipantDTO> getMeetingParticipants(Meeting meeting) {
        try {
            List<Participant> participants = participantRepository.findByMeetingAndLeftAtIsNull(meeting);
            List<ParticipantDTO> participantDTOS = new ArrayList<>();
            for (Participant participant : participants) {
                participantDTOS.add(mapToDTO(participant));
            }
            return participantDTOS;
        } catch (Exception e) {
            log.error("Error getting meeting participants: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Update participant camera status
     */
    public ParticipantDTO updateCameraStatus(String participantUUID, Boolean cameraEnabled) {
        try {
            Optional<Participant> participant = participantRepository.findByParticipantUUID(participantUUID);
            if (participant.isEmpty()) {
                throw new RuntimeException("Participant not found");
            }

            Participant existingParticipant = participant.get();
            existingParticipant.setCameraEnabled(cameraEnabled);
            existingParticipant = participantRepository.save(existingParticipant);

            return mapToDTO(existingParticipant);
        } catch (Exception e) {
            log.error("Error updating camera status: {}", e.getMessage());
            throw new RuntimeException("Failed to update camera status: " + e.getMessage());
        }
    }

    /**
     * Update participant microphone status
     */
    public ParticipantDTO updateMicrophoneStatus(String participantUUID, Boolean microphoneEnabled) {
        try {
            Optional<Participant> participant = participantRepository.findByParticipantUUID(participantUUID);
            if (participant.isEmpty()) {
                throw new RuntimeException("Participant not found");
            }

            Participant existingParticipant = participant.get();
            existingParticipant.setMicrophoneEnabled(microphoneEnabled);
            existingParticipant = participantRepository.save(existingParticipant);

            return mapToDTO(existingParticipant);
        } catch (Exception e) {
            log.error("Error updating microphone status: {}", e.getMessage());
            throw new RuntimeException("Failed to update microphone status: " + e.getMessage());
        }
    }

    /**
     * Update screen sharing status
     */
    public ParticipantDTO updateScreenSharingStatus(String participantUUID, Boolean screenSharingEnabled) {
        try {
            Optional<Participant> participant = participantRepository.findByParticipantUUID(participantUUID);
            if (participant.isEmpty()) {
                throw new RuntimeException("Participant not found");
            }

            Participant existingParticipant = participant.get();
            existingParticipant.setScreenSharingEnabled(screenSharingEnabled);
            existingParticipant = participantRepository.save(existingParticipant);

            return mapToDTO(existingParticipant);
        } catch (Exception e) {
            log.error("Error updating screen sharing status: {}", e.getMessage());
            throw new RuntimeException("Failed to update screen sharing status: " + e.getMessage());
        }
    }

    /**
     * Map Participant entity to ParticipantDTO
     */
    private ParticipantDTO mapToDTO(Participant participant) {
        return ParticipantDTO.builder()
                .id(participant.getId())
                .participantName(participant.getParticipantName())
                .participantEmail(participant.getParticipantEmail())
                .cameraEnabled(participant.getCameraEnabled())
                .microphoneEnabled(participant.getMicrophoneEnabled())
                .screenSharingEnabled(participant.getScreenSharingEnabled())
                .joinedAt(participant.getJoinedAt())
                .leftAt(participant.getLeftAt())
                .isHost(participant.getIsHost())
                .isMuted(participant.getIsMuted())
                .participantUUID(participant.getParticipantUUID())
                .build();
    }
}
