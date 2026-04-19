package com.nestchat.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendVoiceMessageRequest {

    @NotBlank
    private String conversationId;
    @NotBlank
    private String voiceFileId;
    private Integer durationSeconds;
    private String clientMessageId;
}
