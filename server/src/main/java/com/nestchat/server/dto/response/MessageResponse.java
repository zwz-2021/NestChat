package com.nestchat.server.dto.response;

import lombok.Data;

@Data
public class MessageResponse {

    private String messageId;
    private String conversationId;
    private String senderType;
    private String messageType;
    private String content;
    private String imageUrl;
    private String voiceUrl;
    private Integer durationSeconds;
    private String createdAt;
    private String clientMessageId;
    private String sendStatus;
}
