package com.nestchat.server.dto.response;

import lombok.Data;

@Data
public class CaptchaResponse {

    private String captchaId;
    private String imageBase64;
    private Long expireAt;
}
