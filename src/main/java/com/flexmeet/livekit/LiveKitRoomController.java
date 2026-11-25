package com.flexmeet.livekit;

import com.flexmeet.service.auth.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/legacy-rooms")
@CrossOrigin(origins = "${flexmeet.web-origin:https://flexmeet-aiman-3859.vercel.app}",
        allowedHeaders = {"Authorization", "Content-Type"}, methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS
})
public class LiveKitRoomController {
    private final LiveKitRoomService rooms;
    private final JwtTokenProvider jwt;

    @Value("${livekit.url:}")
    private String liveKitUrl;

    public LiveKitRoomController(LiveKitRoomService rooms, JwtTokenProvider jwt) {
        this.rooms = rooms;
        this.jwt = jwt;
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of("ready", liveKitUrl != null && !liveKitUrl.isBlank());
    }

    @PostMapping
    public Map<String, Object> create(@RequestHeader(value = "Authorization", required = false) String auth,
                                      @RequestBody CreateRoom request) {
        String owner = authenticatedUser(auth);
        return rooms.createRoom(request.title, request.mode, owner);
    }

    @PostMapping("/{room}/join")
    public Map<String, Object> join(@PathVariable String room, @RequestBody JoinRoom request,
                                    @RequestHeader(value = "Authorization", required = false) String auth) {
        return rooms.join(room, request.participantName, request.identity, optionalUser(auth));
    }

    @PostMapping("/{room}/mode")
    public Map<String, Object> mode(@PathVariable String room,
                                    @RequestHeader(value = "Authorization", required = false) String auth,
                                    @RequestBody ModeRequest request) {
        return rooms.setMode(room, authenticatedUser(auth), request.mode);
    }

    @GetMapping("/{room}/participants")
    public List<Map<String, Object>> participants(@PathVariable String room,
                                    @RequestHeader(value = "Authorization", required = false) String auth) {
        return rooms.listParticipants(room, authenticatedUser(auth));
    }

    @PostMapping("/{room}/participants/{identity}/permissions")
    public ResponseEntity<Void> permissions(@PathVariable String room, @PathVariable String identity,
                                    @RequestHeader(value = "Authorization", required = false) String auth,
                                    @RequestBody PermissionRequest request) {
        rooms.setParticipantPermissions(room, authenticatedUser(auth), identity, request.audio, request.video);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{room}/participants/{identity}/kick")
    public ResponseEntity<Void> kick(@PathVariable String room, @PathVariable String identity,
                                    @RequestHeader(value = "Authorization", required = false) String auth) {
        rooms.removeParticipant(room, authenticatedUser(auth), identity);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{room}/mute-all")
    public ResponseEntity<Void> muteAll(@PathVariable String room,
                                    @RequestHeader(value = "Authorization", required = false) String auth) {
        rooms.muteAll(room, authenticatedUser(auth));
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(LiveKitRoomService.RoomAccessException.class)
    public ResponseEntity<Map<String, String>> forbidden(RuntimeException error) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", error.getMessage()));
    }

    @ExceptionHandler(LiveKitRoomStore.RoomNotFoundException.class)
    public ResponseEntity<Map<String, String>> missing(RuntimeException error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Meeting not found"));
    }

    @ExceptionHandler(LiveKitRoomService.LiveKitUnavailableException.class)
    public ResponseEntity<Map<String, String>> unavailable(RuntimeException error) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", error.getMessage()));
    }

    private String authenticatedUser(String auth) {
        String user = optionalUser(auth);
        if (user == null) throw new LiveKitRoomService.RoomAccessException("Sign in with a valid account first");
        return user;
    }

    private String optionalUser(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        String token = auth.substring(7).trim();
        return !token.isEmpty() && Boolean.TRUE.equals(jwt.validateToken(token))
                ? jwt.getUserIdFromToken(token) : null;
    }

    public static class CreateRoom { public String title; public String mode; }
    public static class JoinRoom { public String participantName; public String identity; }
    public static class ModeRequest { public String mode; }
    public static class PermissionRequest { public boolean audio; public boolean video; }
}
