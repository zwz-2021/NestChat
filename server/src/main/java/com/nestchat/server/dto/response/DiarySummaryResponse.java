package com.nestchat.server.dto.response;

import lombok.Data;

@Data
public class DiarySummaryResponse {

    private String diaryId;
    private String date;
    private String authorType;
    private String moodText;
    private String contentSummary;
    private Integer imageCount;
    private String coverUrl;
}
