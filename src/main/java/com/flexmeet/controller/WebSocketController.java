package com.flexmeet.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Handle WebRTC signaling (offer/answer)
     */
    @MessageMapping("/meeting/signal")
    @SendTo("/topic/meeting/signal")
    public Map<String, Object> handleSignaling(@Payload Map<String, Object> message) {
        try {
            String meetingCode = (String) message.get("meetingCode");
            String fromParticipantId = (String) message.get("fromParticipantId");
            String toParticipantId = (String) message.get("toParticipantId");
            String type = (String) message.get("type"); // offer, answer, ice-candidate

            log.info("Signaling message: {} from {} to {}", type, fromParticipantId, toParticipantId);

            // Route the message to specific participant
            messagingTemplate.convertAndSendToUser(
                    toParticipantId,
                    "/queue/signal",
                    message
            );

            return message;
        } catch (Exception e) {
            log.error("Error handling signaling message: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Notify participant status change (camera on/off, mic on/off)
     */
    @MessageMapping("/meeting/status-update")
    @SendTo("/topic/meeting/status")
    public Map<String, Object> handleStatusUpdate(@Payload Map<String, Object> message) {
        try {
            String meetingCode = (String) message.get("meetingCode");
            String participantId = (String) message.get("participantId");
            String status = (String) message.get("status");

            log.info("Status update: {} for participant {} in meeting {}", status, participantId, meetingCode);
            return message;
        } catch (Exception e) {
            log.error("Error handling status update: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Handle chat messages
     */
    @MessageMapping("/meeting/chat")
    @SendTo("/topic/meeting/chat")
    public Map<String, Object> handleChatMessage(@Payload Map<String, Object> message) {
        try {
            String meetingCode = (String) message.get("meetingCode");
            String participantName = (String) message.get("participantName");
            String text = (String) message.get("text");
            long timestamp = System.currentTimeMillis();

            message.put("timestamp", timestamp);
            log.info("Chat message from {} in meeting {}: {}", participantName, meetingCode, text);

            return message;
        } catch (Exception e) {
            log.error("Error handling chat message: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Handle screen share notification
     */
    @MessageMapping("/meeting/screen-share")
    @SendTo("/topic/meeting/screen-share")
    public Map<String, Object> handleScreenShare(@Payload Map<String, Object> message) {
        try {
            String meetingCode = (String) message.get("meetingCode");
            String participantId = (String) message.get("participantId");
            Boolean isSharing = (Boolean) message.get("isSharing");

            log.info("Screen share: {} for participant {} in meeting {}", isSharing, participantId, meetingCode);
            return message;
        } catch (Exception e) {
            log.error("Error handling screen share: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Participant joined notification
     */
    @MessageMapping("/meeting/participant-joined")
    @SendTo("/topic/meeting/participants")
    public Map<String, Object> handleParticipantJoined(@Payload Map<String, Object> message) {
        try {
            String meetingCode = (String) message.get("meetingCode");
            String participantId = (String) message.get("participantId");
            String participantName = (String) message.get("participantName");

            log.info("Participant {} joined meeting {}", participantName, meetingCode);
            return message;
        } catch (Exception e) {
            log.error("Error handling participant joined: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Participant left notification
     */
    @MessageMapping("/meeting/participant-left")
    @SendTo("/topic/meeting/participants")
    public Map<String, Object> handleParticipantLeft(@Payload Map<String, Object> message) {
        try {
            String meetingCode = (String) message.get("meetingCode");
            String participantId = (String) message.get("participantId");

            log.info("Participant {} left meeting {}", participantId, meetingCode);
            return message;
        } catch (Exception e) {
            log.error("Error handling participant left: {}", e.getMessage());
            return null;
        }
    }
}
