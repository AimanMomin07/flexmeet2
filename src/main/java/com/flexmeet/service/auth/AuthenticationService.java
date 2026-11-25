package com.flexmeet.service.auth;

import com.flexmeet.model.entity.User;
import com.flexmeet.model.request.AuthenticationRequest;
import com.flexmeet.model.response.AuthenticationResponse;
import com.flexmeet.repository.UserRepository;
import com.flexmeet.service.firebase.FirebaseService;
import com.flexmeet.model.dto.UserDTO;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@Slf4j
@Transactional
public class AuthenticationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FirebaseService firebaseService;

    /**
     * Register a new user
     */
    public AuthenticationResponse register(AuthenticationRequest request) {
        try {
            // Check if user already exists
            if (userRepository.existsByEmail(request.getEmail())) {
                return AuthenticationResponse.builder()
                        .success(false)
                        .message("Email already exists")
                        .build();
            }

            // Create Firebase user
            UserRecord firebaseUser = firebaseService.createFirebaseUser(
                    request.getEmail(),
                    request.getPassword(),
                    request.getFullName()
            );

            // Create local user
            User user = User.builder()
                    .email(request.getEmail())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .fullName(request.getFullName())
                    .firebaseUid(firebaseUser.getUid())
                    .isActive(true)
                    .build();

            user = userRepository.save(user);

            // Generate JWT token
            String token = jwtTokenProvider.generateToken(
                    user.getId().toString(),
                    user.getEmail(),
                    user.getFullName()
            );

            return AuthenticationResponse.builder()
                    .success(true)
                    .message("User registered successfully")
                    .token(token)
                    .user(mapToDTO(user))
                    .build();

        } catch (FirebaseAuthException e) {
            log.error("Firebase authentication error during registration: {}", e.getMessage());
            return AuthenticationResponse.builder()
                    .success(false)
                    .message("Registration failed: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Error during user registration: {}", e.getMessage());
            return AuthenticationResponse.builder()
                    .success(false)
                    .message("Registration failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Login user
     */
    public AuthenticationResponse login(AuthenticationRequest request) {
        try {
            Optional<User> user = userRepository.findByEmail(request.getEmail());

            if (user.isEmpty()) {
                return AuthenticationResponse.builder()
                        .success(false)
                        .message("User not found")
                        .build();
            }

            User existingUser = user.get();

            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), existingUser.getPasswordHash())) {
                return AuthenticationResponse.builder()
                        .success(false)
                        .message("Invalid credentials")
                        .build();
            }

            // Generate JWT token
            String token = jwtTokenProvider.generateToken(
                    existingUser.getId().toString(),
                    existingUser.getEmail(),
                    existingUser.getFullName()
            );

            return AuthenticationResponse.builder()
                    .success(true)
                    .message("Login successful")
                    .token(token)
                    .user(mapToDTO(existingUser))
                    .build();

        } catch (Exception e) {
            log.error("Error during user login: {}", e.getMessage());
            return AuthenticationResponse.builder()
                    .success(false)
                    .message("Login failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Get user by ID
     */
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * Get user by email
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Update user profile
     */
    public AuthenticationResponse updateUserProfile(Long userId, User updatedUser) {
        try {
            Optional<User> user = userRepository.findById(userId);

            if (user.isEmpty()) {
                return AuthenticationResponse.builder()
                        .success(false)
                        .message("User not found")
                        .build();
            }

            User existingUser = user.get();
            existingUser.setFullName(updatedUser.getFullName());
            existingUser.setProfilePictureUrl(updatedUser.getProfilePictureUrl());

            userRepository.save(existingUser);

            return AuthenticationResponse.builder()
                    .success(true)
                    .message("Profile updated successfully")
                    .user(mapToDTO(existingUser))
                    .build();

        } catch (Exception e) {
            log.error("Error updating user profile: {}", e.getMessage());
            return AuthenticationResponse.builder()
                    .success(false)
                    .message("Profile update failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Map User entity to UserDTO
     */
    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .firebaseUid(user.getFirebaseUid())
                .build();
    }
}
