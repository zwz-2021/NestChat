package com.nestchat.server.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class MoodTrendResponse {

    private List<MoodPoint> points;

    @Data
    public static class MoodPoint {
        private String date;
        private Integer score;
        private String moodText;
    }
}
