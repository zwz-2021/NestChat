package com.nestchat.server.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class DiaryDetailResponse {

    private String diaryId;
    private String date;
    private String authorType;
    private String moodCode;
    private String moodText;
    private String content;
    private List<String> imageUrls;
    private String emotionSummary;
    private String triggerEvent;
    private String messageToPartner;
}
