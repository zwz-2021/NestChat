package com.nestchat.server.dto.response;

import lombok.Data;

@Data
public class OptimizeMessageResponse {

    private String optimizedText;
    private String mode;
}