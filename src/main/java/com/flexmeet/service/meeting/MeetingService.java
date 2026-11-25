package com.flexmeet.service.meeting;

import com.flexmeet.model.entity.Meeting;
import com.flexmeet.model.entity.User;
import com.flexmeet.model.dto.MeetingDTO;
import com.flexmeet.model.request.CreateMeetingRequest;
import com.flexmeet.repository.MeetingRepository;
import com.flexmeet.repository.ParticipantRepository;
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
public class MeetingService {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private FirebaseService firebaseService;

    /**
     * Create a new meeting
     */
    public MeetingDTO createMeeting(User host, CreateMeetingRequest request) {
        try {
            String meetingCode = generateMeetingCode();

            Meeting meeting = Meeting.builder()
                    .meetingCode(meetingCode)
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .host(host)
                    .startTime(request.getStartTime() != null ? request.getStartTime() : LocalDateTime.now())
                    .endTime(request.getEndTime())
                    .maxParticipants(request.getMaxParticipants() != null ? request.getMaxParticipants() : 100)
                    .password(request.getPassword())
                    .allowRecording(request.getAllowRecording())
                    .allowScreenShare(request.getAllowScreenShare())
                    .allowChat(request.getAllowChat())
                    .isActive(true)
                    .build();

            meeting = meetingRepository.save(meeting);

            // Store meeting in Firebase
            Map<String, Object> meetingData = new HashMap<>();
            meetingData.put("meetingCode", meeting.getMeetingCode());
            meetingData.put("title", meeting.getTitle());
            meetingData.put("hostId", host.getFirebaseUid());
            meetingData.put("createdAt", System.currentTimeMillis());
            meetingData.put("isActive", true);
            meetingData.put("participants", new HashMap<>());

            firebaseService.storeMeetingData(meetingCode, meetingData)
                    .thenAccept(v -> log.info("Meeting stored in Firebase: " + meetingCode))
                    .exceptionally(e -> {
                        log.error("Failed to store meeting in Firebase: " + e.getMessage());
                        return null;
                    });

            return mapToDTO(meeting, 0);

        } catch (Exception e) {
            log.error("Error creating meeting: {}", e.getMessage());
            throw new RuntimeException("Failed to create meeting: " + e.getMessage());
        }
    }

    /**
     * Get meeting by code
     */
    public Optional<MeetingDTO> getMeetingByCode(String meetingCode) {
        try {
            Optional<Meeting> meeting = meetingRepository.findByMeetingCode(meetingCode);
            if (meeting.isPresent()) {
                Integer participantCount = participantRepository.countByMeetingAndLeftAtIsNull(meeting.get());
                return Optional.of(mapToDTO(meeting.get(), participantCount));
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error getting meeting by code: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get meeting by ID
     */
    public Optional<MeetingDTO> getMeetingById(Long meetingId) {
        try {
            Optional<Meeting> meeting = meetingRepository.findById(meetingId);
            if (meeting.isPresent()) {
                Integer participantCount = participantRepository.countByMeetingAndLeftAtIsNull(meeting.get());
                return Optional.of(mapToDTO(meeting.get(), participantCount));
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error getting meeting by ID: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get user's meetings
     */
    public List<MeetingDTO> getUserMeetings(User user) {
        try {
            List<Meeting> meetings = meetingRepository.findByHost(user);
            List<MeetingDTO> meetingDTOS = new ArrayList<>();
            for (Meeting meeting : meetings) {
                Integer participantCount = participantRepository.countByMeetingAndLeftAtIsNull(meeting);
                meetingDTOS.add(mapToDTO(meeting, participantCount));
            }
            return meetingDTOS;
        } catch (Exception e) {
            log.error("Error getting user meetings: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Update meeting
     */
    public MeetingDTO updateMeeting(Long meetingId, CreateMeetingRequest request) {
        try {
            Optional<Meeting> meeting = meetingRepository.findById(meetingId);
            if (meeting.isEmpty()) {
                throw new RuntimeException("Meeting not found");
            }

            Meeting existingMeeting = meeting.get();
            existingMeeting.setTitle(request.getTitle());
            existingMeeting.setDescription(request.getDescription());
            existingMeeting.setMaxParticipants(request.getMaxParticipants());
            existingMeeting.setPassword(request.getPassword());
            existingMeeting.setAllowRecording(request.getAllowRecording());
            existingMeeting.setAllowScreenShare(request.getAllowScreenShare());
            existingMeeting.setAllowChat(request.getAllowChat());

            existingMeeting = meetingRepository.save(existingMeeting);

            Integer participantCount = participantRepository.countByMeetingAndLeftAtIsNull(existingMeeting);
            return mapToDTO(existingMeeting, participantCount);

        } catch (Exception e) {
            log.error("Error updating meeting: {}", e.getMessage());
            throw new RuntimeException("Failed to update meeting: " + e.getMessage());
        }
    }

    /**
     * End meeting
     */
    public void endMeeting(String meetingCode) {
        try {
            Optional<Meeting> meeting = meetingRepository.findByMeetingCode(meetingCode);
            if (meeting.isPresent()) {
                Meeting existingMeeting = meeting.get();
                existingMeeting.setIsActive(false);
                existingMeeting.setEndTime(LocalDateTime.now());
                meetingRepository.save(existingMeeting);
                log.info("Meeting ended: " + meetingCode);
            }
        } catch (Exception e) {
            log.error("Error ending meeting: {}", e.getMessage());
        }
    }

    /**
     * Generate unique meeting code
     */
    private String generateMeetingCode() {
        String meetingCode;
        do {
            meetingCode = UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        } while (meetingRepository.findByMeetingCode(meetingCode).isPresent());
        return meetingCode;
    }

    /**
     * Map Meeting entity to MeetingDTO
     */
    private MeetingDTO mapToDTO(Meeting meeting, Integer participantCount) {
        return MeetingDTO.builder()
                .id(meeting.getId())
                .meetingCode(meeting.getMeetingCode())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .maxParticipants(meeting.getMaxParticipants())
                .isActive(meeting.getIsActive())
                .allowRecording(meeting.getAllowRecording())
                .allowScreenShare(meeting.getAllowScreenShare())
                .allowChat(meeting.getAllowChat())
                .createdAt(meeting.getCreatedAt())
                .currentParticipants(participantCount)
                .build();
    }
}
