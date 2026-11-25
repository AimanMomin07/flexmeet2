package com.flexmeet.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long id;
    private String email;
    private String fullName;
    private String profilePictureUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String firebaseUid;
}
