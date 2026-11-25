package com.flexmeet.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinMeetingRequest {

    private String meetingCode;
    private String password;
    private String participantName;
    private Boolean cameraEnabled;
    private Boolean microphoneEnabled;
}
