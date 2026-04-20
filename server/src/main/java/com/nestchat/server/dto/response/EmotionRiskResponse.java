package com.nestchat.server.dto.response;

import lombok.Data;

@Data
public class EmotionRiskResponse {

    private boolean shouldPrompt;
    private String riskLevel;
    private String emotionTag;
    private String title;
    private String message;
}
