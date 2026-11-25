package com.flexmeet.livekit;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LiveKitRoomStore {
    public static final String OPEN = "OPEN";
    public static final String HOST_ONLY = "HOST_ONLY";

    private final SecureRandom random = new SecureRandom();
    private final Map<String, RoomState> rooms = new ConcurrentHashMap<>();

    public RoomState create(String title, String mode, String ownerUserId) {
        if (ownerUserId == null || ownerUserId.trim().isEmpty()) {
            throw new LiveKitRoomService.RoomAccessException("A verified account is required to host a room");
        }
        String safeMode = mode == null || mode.trim().isEmpty() ? OPEN : mode;
        if (!OPEN.equals(safeMode) && !HOST_ONLY.equals(safeMode)) {
            throw new LiveKitRoomService.RoomAccessException("Unknown room mode");
        }
        String code;
        do {
            byte[] bytes = new byte[9];
            random.nextBytes(bytes);
            code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).toUpperCase();
        } while (rooms.containsKey(code));
        RoomState state = new RoomState(code, title, safeMode,
                java.util.UUID.randomUUID().toString(), ownerUserId);
        rooms.put(code, state);
        return state;
    }

    public RoomState require(String room) {
        RoomState state = rooms.get(room);
        if (state == null) throw new RoomNotFoundException();
        return state;
    }

    public static class RoomState {
        public final String code;
        public final String title;
        public volatile String mode;
        public final String hostIdentity;
        public final String ownerUserId;
        public final Set<String> bannedIdentities = ConcurrentHashMap.newKeySet();
        public final Map<String, MediaPermission> permissions = new ConcurrentHashMap<>();

        RoomState(String code, String title, String mode, String hostIdentity, String ownerUserId) {
            this.code = code;
            this.title = title;
            this.mode = mode;
            this.hostIdentity = hostIdentity;
            this.ownerUserId = ownerUserId;
        }
    }

    public static class MediaPermission {
        public volatile boolean audio;
        public volatile boolean video;

        public MediaPermission(boolean audio, boolean video) {
            this.audio = audio;
            this.video = video;
        }
    }

    public static class RoomNotFoundException extends RuntimeException {}
}
