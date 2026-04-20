package com.nestchat.server.dto.response;

import lombok.Data;

@Data
public class ChatSessionResponse {

    private String conversationId;
    private String partnerUserId;
    private String partnerNickname;
    private String partnerAvatarUrl;
    private String subtitle;
    private String partnerMoodCode;
    private String partnerMoodText;
    private String partnerLastActiveAt;
    private boolean partnerTodayDiary;
    private int companionDays;
}
