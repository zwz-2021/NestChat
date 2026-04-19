package com.nestchat.server.dto.response;

import lombok.Data;

@Data
public class ProfileResponse {

    private String userId;
    private String account;
    private String nickname;
    private String avatarUrl;
    private String moodCode;
    private String moodText;
}
