package com.nestchat.server.dto.response;

import lombok.Data;

@Data
public class UserBriefResponse {

    private String userId;
    private String account;
    private String nickname;
    private String avatarUrl;
}
