package com.nestchat.server.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    private String nickname;
    private String avatarUrl;
}
