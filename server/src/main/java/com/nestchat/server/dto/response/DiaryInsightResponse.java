package com.nestchat.server.dto.response;

import lombok.Data;

@Data
public class DiaryInsightResponse {

    private String emotionSummary;
    private String triggerEvent;
    private String messageToPartner;
}
