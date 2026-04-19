package com.nestchat.server.dto.response;

import lombok.Data;

@Data
public class ChatSessionResponse {

    private String conversationId;
    private String partnerUserId;
    private String partnerNickname;
    private String partnerAvatarUrl;
    private String subtitle;
}
