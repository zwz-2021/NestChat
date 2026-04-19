package com.nestchat.server.dto.response;

import lombok.Data;

@Data
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private Long expireAt;
    private UserBriefResponse user;
}
