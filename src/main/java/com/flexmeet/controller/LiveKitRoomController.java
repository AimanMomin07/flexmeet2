package com.flexmeet.controller;

import com.flexmeet.livekit.LiveKitRoomService;
import com.flexmeet.livekit.LiveKitRoomStore;
import com.flexmeet.service.auth.JwtTokenProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rooms")
@CrossOrigin(origins = "${flexmeet.web-origin:https://flexmeet-aiman-3859.vercel.app}", maxAge = 3600,
        allowedHeaders = {"Authorization", "Content-Type"})
public class LiveKitRoomController {
    private final LiveKitRoomService roomService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${livekit.url:}")
    private String liveKitUrl;

    public LiveKitRoomController(LiveKitRoomService roomService, JwtTokenProvider jwtTokenProvider) {
        this.roomService = roomService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of("ready", liveKitUrl != null && !liveKitUrl.isBlank());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createRoom(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody CreateRoomRequest request) {
        String ownerUserId = requireUserId(authorization);
        String mode = request.mode() == null ? LiveKitRoomStore.OPEN : request.mode().toUpperCase();
        if (!LiveKitRoomStore.OPEN.equals(mode) && !LiveKitRoomStore.HOST_ONLY.equals(mode)) {
            return ResponseEntity.badRequest().body(Map.of("message", "mode must be OPEN or HOST_ONLY"));
        }
        return ResponseEntity.ok(roomService.createRoom(request.title(), mode, ownerUserId));
    }

    @PostMapping("/{room}/join")
    public ResponseEntity<Map<String, Object>> join(@PathVariable String room,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) JoinRoomRequest request) {
        JoinRoomRequest body = request == null ? new JoinRoomRequest(null, null) : request;
        return ResponseEntity.ok(roomService.join(room, body.participantName(), body.identity(),
                optionalUserId(authorization)));
    }

    @PostMapping("/{room}/mode")
    public ResponseEntity<Map<String, Object>> setMode(@PathVariable String room,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody SetModeRequest request) {
        return ResponseEntity.ok(roomService.setMode(room, requireUserId(authorization),
                request.mode() == null ? "" : request.mode().toUpperCase()));
    }

    @GetMapping("/{room}/participants")
    public List<Map<String, Object>> participants(@PathVariable String room,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return roomService.listParticipants(room, requireUserId(authorization));
    }

    @PostMapping("/{room}/participants/{identity}/permissions")
    public ResponseEntity<Map<String, Object>> permissions(@PathVariable String room,
            @PathVariable String identity,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody PermissionRequest request) {
        roomService.setParticipantPermissions(room, requireUserId(authorization), identity,
                Boolean.TRUE.equals(request.audio()), Boolean.TRUE.equals(request.video()));
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{room}/participants/{identity}/kick")
    public ResponseEntity<Map<String, Object>> kick(@PathVariable String room,
            @PathVariable String identity,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        roomService.removeParticipant(room, requireUserId(authorization), identity);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{room}/mute-all")
    public ResponseEntity<Map<String, Object>> muteAll(@PathVariable String room,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        roomService.muteAll(room, requireUserId(authorization));
        return ResponseEntity.ok(Map.of("success", true));
    }

    private String requireUserId(String authorization) {
        String userId = optionalUserId(authorization);
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in required");
        return userId;
    }

    private String optionalUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isEmpty()) return null;
        if (Boolean.TRUE.equals(jwtTokenProvider.validateToken(token))) {
            return jwtTokenProvider.getUserIdFromToken(token);
        }
        try {
            FirebaseToken firebaseToken = FirebaseAuth.getInstance().verifyIdToken(token);
            return firebaseToken.getUid();
        } catch (Exception ignored) {
            return null;
        }
    }

    @ExceptionHandler(LiveKitRoomService.RoomAccessException.class)
    public ResponseEntity<Map<String, String>> accessDenied(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler({LiveKitRoomStore.RoomNotFoundException.class, LiveKitRoomService.RoomNotFoundException.class})
    public ResponseEntity<Map<String, String>> notFound(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Meeting not found"));
    }

    @ExceptionHandler(LiveKitRoomService.LiveKitUnavailableException.class)
    public ResponseEntity<Map<String, String>> unavailable(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", exception.getMessage()));
    }

    public record CreateRoomRequest(String title, String mode) {}
    public record JoinRoomRequest(String participantName, String identity) {}
    public record SetModeRequest(String mode) {}
    public record PermissionRequest(Boolean audio, Boolean video) {}
}
