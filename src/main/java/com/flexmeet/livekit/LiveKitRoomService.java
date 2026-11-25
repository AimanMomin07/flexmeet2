package com.flexmeet.livekit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LiveKitRoomService {
    private final LiveKitRoomStore store;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${livekit.url:}")
    private String liveKitUrl;
    @Value("${livekit.api-key:}")
    private String apiKey;
    @Value("${livekit.api-secret:}")
    private String apiSecret;

    public LiveKitRoomService(LiveKitRoomStore store, ObjectMapper objectMapper) {
        this.store = store;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> createRoom(String title, String mode, String ownerUserId) {
        requireConfigured();
        if (ownerUserId == null || ownerUserId.trim().isEmpty()) {
            throw new RoomAccessException("A verified account is required to host a room");
        }
        String safeTitle = title == null || title.trim().isEmpty() ? "FlexMeet room" : title.trim();
        LiveKitRoomStore.RoomState state = store.create(safeTitle, mode, ownerUserId);
        Map<String, Object> response = new HashMap<>();
        response.put("room", state.code);
        response.put("title", state.title);
        response.put("mode", state.mode);
        response.put("identity", state.hostIdentity);
        response.put("hostIdentity", state.hostIdentity);
        response.put("url", liveKitUrl);
        response.put("token", createParticipantToken(state, state.hostIdentity, "Host", true));
        return response;
    }

    public Map<String, Object> join(String roomCode, String participantName, String identity, String accountUserId) {
        requireConfigured();
        LiveKitRoomStore.RoomState state = store.require(roomCode);
        String participantIdentity = identity == null || identity.trim().isEmpty()
                ? UUID.randomUUID().toString() : identity.trim();
        boolean isHost = state.ownerUserId.equals(accountUserId);
        if (isHost) participantIdentity = state.hostIdentity;
        if (state.bannedIdentities.contains(participantIdentity)) {
            throw new RoomAccessException("You have been removed from this meeting");
        }
        LiveKitRoomStore.MediaPermission override = state.permissions.get(participantIdentity);
        boolean canPublishAudio = isHost || (override != null ? override.audio : LiveKitRoomStore.OPEN.equals(state.mode));
        boolean canPublishVideo = isHost || (override != null ? override.video : LiveKitRoomStore.OPEN.equals(state.mode));
        String displayName = participantName == null || participantName.trim().isEmpty()
                ? "Guest" : participantName.trim();
        Map<String, Object> response = new HashMap<>();
        response.put("room", state.code);
        response.put("title", state.title);
        response.put("mode", state.mode);
        response.put("identity", participantIdentity);
        response.put("hostIdentity", state.hostIdentity);
        response.put("isHost", isHost);
        response.put("canPublishAudio", canPublishAudio);
        response.put("canPublishVideo", canPublishVideo);
        response.put("url", liveKitUrl);
        response.put("token", createParticipantToken(state, participantIdentity, displayName, isHost,
                canPublishAudio, canPublishVideo));
        return response;
    }

    public Map<String, Object> setMode(String roomCode, String ownerUserId, String mode) {
        LiveKitRoomStore.RoomState state = authorizedRoom(roomCode, ownerUserId);
        if (!LiveKitRoomStore.OPEN.equals(mode) && !LiveKitRoomStore.HOST_ONLY.equals(mode)) {
            throw new RoomAccessException("Unknown room mode");
        }
        state.mode = mode;
        if (!LiveKitRoomStore.OPEN.equals(mode)) state.permissions.clear();
        updateAllParticipantPermissions(state);
        return Map.of("room", roomCode, "mode", mode);
    }

    public void setParticipantPermissions(String roomCode, String ownerUserId, String identity,
                                           boolean audio, boolean video) {
        LiveKitRoomStore.RoomState state = authorizedRoom(roomCode, ownerUserId);
        if (state.hostIdentity.equals(identity)) throw new RoomAccessException("Host permissions cannot be changed here");
        state.permissions.put(identity, new LiveKitRoomStore.MediaPermission(audio, video));
        updateParticipantPermission(state, identity, audio, video);
    }

    public void removeParticipant(String roomCode, String ownerUserId, String identity) {
        LiveKitRoomStore.RoomState state = authorizedRoom(roomCode, ownerUserId);
        if (state.hostIdentity.equals(identity)) throw new RoomAccessException("The host cannot remove themselves");
        state.bannedIdentities.add(identity);
        Map<String, Object> body = new HashMap<>();
        body.put("room", state.code);
        body.put("identity", identity);
        callRoomService(state, "RemoveParticipant", body);
    }

    public List<Map<String, Object>> listParticipants(String roomCode, String ownerUserId) {
        LiveKitRoomStore.RoomState state = authorizedRoom(roomCode, ownerUserId);
        Map<String, Object> result = callRoomService(state, "ListParticipants", Map.of("room", state.code));
        Object participants = result.get("participants");
        if (participants instanceof List<?>) {
            return objectMapper.convertValue(participants, new TypeReference<List<Map<String, Object>>>() {});
        }
        return List.of();
    }

    public void muteAll(String roomCode, String ownerUserId) {
        LiveKitRoomStore.RoomState state = authorizedRoom(roomCode, ownerUserId);
        for (Map<String, Object> participant : listParticipants(roomCode, ownerUserId)) {
            String identity = String.valueOf(participant.get("identity"));
            if (state.hostIdentity.equals(identity)) continue;
            state.permissions.put(identity, new LiveKitRoomStore.MediaPermission(false, false));
            updateParticipantPermission(state, identity, false, false);
        }
    }

    private LiveKitRoomStore.RoomState authorizedRoom(String roomCode, String ownerUserId) {
        requireConfigured();
        LiveKitRoomStore.RoomState state = store.require(roomCode);
        if (ownerUserId == null || !state.ownerUserId.equals(ownerUserId)) {
            throw new RoomAccessException("Room owner access required");
        }
        return state;
    }

    private void updateAllParticipantPermissions(LiveKitRoomStore.RoomState state) {
        for (Map<String, Object> participant : listParticipants(state.code, state.ownerUserId)) {
            String identity = String.valueOf(participant.get("identity"));
            if (state.hostIdentity.equals(identity)) continue;
            LiveKitRoomStore.MediaPermission permission = state.permissions.get(identity);
            boolean audio = permission != null ? permission.audio : LiveKitRoomStore.OPEN.equals(state.mode);
            boolean video = permission != null ? permission.video : LiveKitRoomStore.OPEN.equals(state.mode);
            updateParticipantPermission(state, identity, audio, video);
        }
    }

    private void updateParticipantPermission(LiveKitRoomStore.RoomState state, String identity,
                                             boolean audio, boolean video) {
        List<String> sources = new ArrayList<>();
        if (audio) sources.add("MICROPHONE");
        if (video) sources.add("CAMERA");
        Map<String, Object> permission = new HashMap<>();
        permission.put("canPublish", audio || video);
        permission.put("canSubscribe", true);
        permission.put("canPublishData", true);
        permission.put("canPublishSources", sources);
        Map<String, Object> body = new HashMap<>();
        body.put("room", state.code);
        body.put("identity", identity);
        body.put("permission", permission);
        callRoomService(state, "UpdateParticipant", body);
    }

    private Map<String, Object> callRoomService(LiveKitRoomStore.RoomState state, String method,
                                                 Map<String, Object> body) {
        try {
            String baseUrl = liveKitUrl.trim().replaceFirst("^wss://", "https://")
                    .replaceFirst("^ws://", "http://").replaceAll("/+$", "");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(createAdminToken(state));
            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            String response = restTemplate.postForObject(
                    baseUrl + "/twirp/livekit.RoomService/" + method, request, String.class);
            if (response == null || response.isEmpty()) return Map.of();
            return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        } catch (Exception exception) {
            throw new LiveKitUnavailableException("LiveKit room operation failed: " + exception.getMessage());
        }
    }

    private String createParticipantToken(LiveKitRoomStore.RoomState state, String identity,
                                          String name, boolean isHost) {
        boolean canPublish = isHost || LiveKitRoomStore.OPEN.equals(state.mode);
        return createParticipantToken(state, identity, name, isHost, canPublish, canPublish);
    }

    private String createParticipantToken(LiveKitRoomStore.RoomState state, String identity,
                                          String name, boolean isHost,
                                          boolean canPublishAudio, boolean canPublishVideo) {
        List<String> sources = new ArrayList<>();
        if (canPublishAudio) sources.add("MICROPHONE");
        if (canPublishVideo) sources.add("CAMERA");
        Map<String, Object> videoGrant = new HashMap<>();
        videoGrant.put("room", state.code);
        videoGrant.put("roomJoin", true);
        videoGrant.put("roomAdmin", isHost);
        videoGrant.put("canPublish", canPublishAudio || canPublishVideo);
        videoGrant.put("canPublishSources", sources);
        videoGrant.put("canSubscribe", true);
        videoGrant.put("canPublishData", true);

        long now = Instant.now().getEpochSecond();
        SecretKey key = Keys.hmacShaKeyFor(apiSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .issuer(apiKey)
                .subject(identity)
                .claim("name", name)
                .claim("video", videoGrant)
                .issuedAt(Date.from(Instant.ofEpochSecond(now)))
                .notBefore(Date.from(Instant.ofEpochSecond(now - 5)))
                .expiration(Date.from(Instant.ofEpochSecond(now + 3600)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private String createAdminToken(LiveKitRoomStore.RoomState state) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> videoGrant = new HashMap<>();
        videoGrant.put("room", state.code);
        videoGrant.put("roomAdmin", true);
        SecretKey key = Keys.hmacShaKeyFor(apiSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .issuer(apiKey)
                .subject("flexmeet-server")
                .claim("video", videoGrant)
                .issuedAt(Date.from(Instant.ofEpochSecond(now)))
                .notBefore(Date.from(Instant.ofEpochSecond(now - 5)))
                .expiration(Date.from(Instant.ofEpochSecond(now + 60)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private void requireConfigured() {
        if (liveKitUrl == null || liveKitUrl.trim().isEmpty()
                || apiKey == null || apiKey.trim().isEmpty()
                || apiSecret == null || apiSecret.trim().isEmpty()) {
            throw new LiveKitUnavailableException(
                    "LiveKit is not configured. Set LIVEKIT_URL, LIVEKIT_API_KEY, and LIVEKIT_API_SECRET.");
        }
    }

    public static class RoomAccessException extends RuntimeException {
        public RoomAccessException(String message) { super(message); }
    }

    public static class RoomNotFoundException extends RuntimeException {}
    public static class LiveKitUnavailableException extends RuntimeException {
        public LiveKitUnavailableException(String message) { super(message); }
    }
}
