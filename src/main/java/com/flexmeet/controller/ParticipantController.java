package com.flexmeet.controller;

import com.flexmeet.model.dto.ParticipantDTO;
import com.flexmeet.model.entity.Meeting;
import com.flexmeet.model.entity.User;
import com.flexmeet.model.request.JoinMeetingRequest;
import com.flexmeet.model.response.ApiResponse;
import com.flexmeet.repository.MeetingRepository;
import com.flexmeet.service.auth.JwtTokenProvider;
import com.flexmeet.service.meeting.ParticipantService;
import com.flexmeet.service.auth.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/participants")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ParticipantController {

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Join a meeting
     */
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<ParticipantDTO>> joinMeeting(@RequestBody JoinMeetingRequest request) {
        try {
            Optional<Meeting> meeting = meetingRepository.findByMeetingCode(request.getMeetingCode());
            
            if (meeting.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse<>(false, "Meeting not found", null));
            }

            // Try to join without authentication (anonymous) or with authentication
            User user = null;
            ParticipantDTO participant = participantService.joinMeeting(meeting.get(), user, request);
            
            log.info("Participant joined meeting: {}", request.getMeetingCode());
            return ResponseEntity.ok(new ApiResponse<>(true, "Joined meeting successfully", participant));

        } catch (Exception e) {
            log.error("Error joining meeting: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(false, "Failed to join meeting: " + e.getMessage(), null));
        }
    }

    /**
     * Join a meeting (authenticated)
     */
    @PostMapping("/join-authenticated")
    public ResponseEntity<ApiResponse<ParticipantDTO>> joinMeetingAuthenticated(
            @RequestHeader("Authorization") String token,
            @RequestBody JoinMeetingRequest request) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            String userId = jwtTokenProvider.getUserIdFromToken(jwtToken);
            
            if (userId == null) {
                return ResponseEntity.ok(new ApiResponse<>(false, "Invalid token", null));
            }

            Long userIdLong = Long.parseLong(userId);
            Optional<User> user = authenticationService.getUserById(userIdLong);

            if (user.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse<>(false, "User not found", null));
            }

            Optional<Meeting> meeting = meetingRepository.findByMeetingCode(request.getMeetingCode());
            
            if (meeting.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse<>(false, "Meeting not found", null));
            }

            ParticipantDTO participant = participantService.joinMeeting(meeting.get(), user.get(), request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Joined meeting successfully", participant));

        } catch (Exception e) {
            log.error("Error joining meeting: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(false, "Failed to join meeting: " + e.getMessage(), null));
        }
    }

    /**
     * Leave a meeting
     */
    @PostMapping("/{participantUUID}/leave")
    public ResponseEntity<ApiResponse<String>> leaveMeeting(
            @PathVariable String participantUUID,
            @RequestParam String meetingCode) {
        try {
            participantService.leaveMeeting(participantUUID, meetingCode);
            log.info("Participant left meeting: {}", meetingCode);
            return ResponseEntity.ok(new ApiResponse<>(true, "Left meeting successfully", participantUUID));

        } catch (Exception e) {
            log.error("Error leaving meeting: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(false, "Failed to leave meeting: " + e.getMessage(), null));
        }
    }

    /**
     * Get all participants in a meeting
     */
    @GetMapping("/meeting/{meetingCode}")
    public ResponseEntity<ApiResponse<List<ParticipantDTO>>> getMeetingParticipants(
            @PathVariable String meetingCode) {
        try {
            Optional<Meeting> meeting = meetingRepository.findByMeetingCode(meetingCode);
            
            if (meeting.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse<>(false, "Meeting not found", null));
            }

            List<ParticipantDTO> participants = participantService.getMeetingParticipants(meeting.get());
            return ResponseEntity.ok(new ApiResponse<>(true, "Participants retrieved successfully", participants));

        } catch (Exception e) {
            log.error("Error getting meeting participants: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(false, "Failed to get participants: " + e.getMessage(), null));
        }
    }

    /**
     * Update camera status
     */
    @PutMapping("/{participantUUID}/camera")
    public ResponseEntity<ApiResponse<ParticipantDTO>> updateCameraStatus(
            @PathVariable String participantUUID,
            @RequestParam Boolean enabled) {
        try {
            ParticipantDTO participant = participantService.updateCameraStatus(participantUUID, enabled);
            return ResponseEntity.ok(new ApiResponse<>(true, "Camera status updated", participant));

        } catch (Exception e) {
            log.error("Error updating camera status: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(false, "Failed to update camera status: " + e.getMessage(), null));
        }
    }

    /**
     * Update microphone status
     */
    @PutMapping("/{participantUUID}/microphone")
    public ResponseEntity<ApiResponse<ParticipantDTO>> updateMicrophoneStatus(
            @PathVariable String participantUUID,
            @RequestParam Boolean enabled) {
        try {
            ParticipantDTO participant = participantService.updateMicrophoneStatus(participantUUID, enabled);
            return ResponseEntity.ok(new ApiResponse<>(true, "Microphone status updated", participant));

        } catch (Exception e) {
            log.error("Error updating microphone status: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(false, "Failed to update microphone status: " + e.getMessage(), null));
        }
    }

    /**
     * Update screen sharing status
     */
    @PutMapping("/{participantUUID}/screen-share")
    public ResponseEntity<ApiResponse<ParticipantDTO>> updateScreenSharingStatus(
            @PathVariable String participantUUID,
            @RequestParam Boolean enabled) {
        try {
            ParticipantDTO participant = participantService.updateScreenSharingStatus(participantUUID, enabled);
            return ResponseEntity.ok(new ApiResponse<>(true, "Screen sharing status updated", participant));

        } catch (Exception e) {
            log.error("Error updating screen sharing status: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(false, "Failed to update screen sharing status: " + e.getMessage(), null));
        }
    }
}
