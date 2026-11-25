package com.flexmeet.controller;

import com.flexmeet.model.dto.MeetingDTO;
import com.flexmeet.model.entity.Meeting;
import com.flexmeet.model.entity.User;
import com.flexmeet.model.request.CreateMeetingRequest;
import com.flexmeet.model.response.ApiResponse;
import com.flexmeet.repository.MeetingRepository;
import com.flexmeet.service.auth.JwtTokenProvider;
import com.flexmeet.service.meeting.MeetingService;
import com.flexmeet.service.auth.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/meetings")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class MeetingController {

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Create a new meeting
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<MeetingDTO>> createMeeting(
            @RequestHeader("Authorization") String token,
            @RequestBody CreateMeetingRequest request) {
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

            MeetingDTO createdMeeting = meetingService.createMeeting(user.get(), request);
            log.info("Meeting created successfully: {}", createdMeeting.getMeetingCode());
            return ResponseEntity.ok(new ApiResponse<>(true, "Meeting created successfully", createdMeeting));

        } catch (Exception e) {
            log.error("Error creating meeting: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(false, "Failed to create meeting: " + e.getMessage(), null));
        }
    }

    /**
     * Get meeting by code
     */
    @GetMapping("/code/{meetingCode}")
    public ResponseEntity<ApiResponse<MeetingDTO>> getMeetingByCode(@PathVariable String meetingCode) {
        try {
            Optional<MeetingDTO> meeting = meetingService.getMeetingByCode(meetingCode);
            if (meeting.isPresent()) {
                return ResponseEntity.ok(new ApiResponse<>(true, "Meeting found", meeting.get()));
            } else {
                return ResponseEntity.ok(new ApiResponse<>(false, "Meeting not found", null));
            }
        } catch (Exception e) {
            log.error("Error getting meeting: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(false, "Failed to get meeting: " + e.getMessage(), null));
        }
    }

    /**
     * Get meeting by ID
     */
    @GetMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<MeetingDTO>> getMeetingById(@PathVariable Long meetingId) {
        try {
            Optional<MeetingDTO> meeting = meetingService.getMeetingById(meetingId);
            if (meeting.isPresent()) {
                return ResponseEntity.ok(new ApiResponse<>(true, "Meeting found", meeting.get()));
            } else {
                return ResponseEntity.ok(new ApiResponse<>(false, "Meeting not found", null));
            }
        } catch (Exception e) {
            log.error("Error getting meeting: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(false, "Failed to get meeting: " + e.getMessage(), null));
        }
    }

    /**
     * Get user's meetings
     */
    @GetMapping("/my/list")
    public ResponseEntity<ApiResponse<List<MeetingDTO>>> getUserMeetings(
            @RequestHeader("Authorization") String token) {
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

            List<MeetingDTO> meetings = meetingService.getUserMeetings(user.get());
            return ResponseEntity.ok(new ApiResponse<>(true, "Meetings retrieved successfully", meetings));

        } catch (Exception e) {
            log.error("Error getting user meetings: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(false, "Failed to get meetings: " + e.getMessage(), null));
        }
    }

    /**
     * Update meeting
     */
    @PutMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<MeetingDTO>> updateMeeting(
            @PathVariable Long meetingId,
            @RequestHeader("Authorization") String token,
            @RequestBody CreateMeetingRequest request) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            String userId = jwtTokenProvider.getUserIdFromToken(jwtToken);
            
            if (userId == null) {
                return ResponseEntity.ok(new ApiResponse<>(false, "Invalid token", null));
            }

            MeetingDTO updatedMeeting = meetingService.updateMeeting(meetingId, request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Meeting updated successfully", updatedMeeting));

        } catch (Exception e) {
            log.error("Error updating meeting: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(false, "Failed to update meeting: " + e.getMessage(), null));
        }
    }

    /**
     * End meeting
     */
    @PostMapping("/{meetingCode}/end")
    public ResponseEntity<ApiResponse<String>> endMeeting(
            @PathVariable String meetingCode,
            @RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            String userId = jwtTokenProvider.getUserIdFromToken(jwtToken);
            
            if (userId == null) {
                return ResponseEntity.ok(new ApiResponse<>(false, "Invalid token", null));
            }

            meetingService.endMeeting(meetingCode);
            log.info("Meeting ended: {}", meetingCode);
            return ResponseEntity.ok(new ApiResponse<>(true, "Meeting ended successfully", meetingCode));

        } catch (Exception e) {
            log.error("Error ending meeting: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(false, "Failed to end meeting: " + e.getMessage(), null));
        }
    }
}
