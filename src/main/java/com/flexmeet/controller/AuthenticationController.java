package com.flexmeet.controller;

import com.flexmeet.model.request.AuthenticationRequest;
import com.flexmeet.model.response.AuthenticationResponse;
import com.flexmeet.model.response.ApiResponse;
import com.flexmeet.model.entity.User;
import com.flexmeet.service.auth.AuthenticationService;
import com.flexmeet.service.auth.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Slf4j
@CrossOrigin(origins = "${flexmeet.web-origin:https://flexmeet-aiman-3859.vercel.app}", allowedHeaders = {"Authorization", "Content-Type"}, maxAge = 3600)
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> register(@RequestBody AuthenticationRequest request) {
        log.info("User registration request: {}", request.getEmail());
        AuthenticationResponse response = authenticationService.register(request);
        return ResponseEntity.ok(new ApiResponse<>(response.getSuccess(), response.getMessage(), response));
    }

    /**
     * Login user
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(@RequestBody AuthenticationRequest request) {
        log.info("User login request: {}", request.getEmail());
        AuthenticationResponse response = authenticationService.login(request);
        return ResponseEntity.ok(new ApiResponse<>(response.getSuccess(), response.getMessage(), response));
    }

    /**
     * Validate token
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Boolean isValid = jwtTokenProvider.validateToken(jwtToken);
            return ResponseEntity.ok(new ApiResponse<>(true, "Token validation successful", isValid));
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(false, "Token validation failed", false));
        }
    }

    /**
     * Get current user info
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Object>> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            String userId = jwtTokenProvider.getUserIdFromToken(jwtToken);
            
            if (userId == null) {
                return ResponseEntity.ok(new ApiResponse<>(false, "Invalid token", null));
            }

            Long userIdLong = Long.parseLong(userId);
            var user = authenticationService.getUserById(userIdLong);
            
            if (user.isPresent()) {
                return ResponseEntity.ok(new ApiResponse<>(true, "User retrieved successfully", user.get()));
            } else {
                return ResponseEntity.ok(new ApiResponse<>(false, "User not found", null));
            }
        } catch (Exception e) {
            log.error("Error getting current user: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(false, "Failed to get user information", null));
        }
    }
}
