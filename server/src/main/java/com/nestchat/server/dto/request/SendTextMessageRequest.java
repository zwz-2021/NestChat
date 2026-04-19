package com.nestchat.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendTextMessageRequest {

    @NotBlank
    private String conversationId;
    @NotBlank
    private String content;
    private String clientMessageId;
}
