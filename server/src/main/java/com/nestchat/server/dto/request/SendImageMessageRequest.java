package com.nestchat.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendImageMessageRequest {

    @NotBlank
    private String conversationId;
    @NotBlank
    private String imageFileId;
    private String clientMessageId;
}
