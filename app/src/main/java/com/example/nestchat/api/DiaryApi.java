package com.example.nestchat.api;

import java.util.List;

/**
 * 日记接口占位。
 */
public interface DiaryApi {

    void getDiaryList(GetDiaryListRequest request, ApiCallback<DiaryListResponse> callback);

    void getDiaryDetail(String diaryId, ApiCallback<DiaryDetailResponse> callback);

    void createDiary(CreateDiaryRequest request, ApiCallback<DiaryDetailResponse> callback);

    void getPartnerMoodTrend(ApiCallback<MoodTrendResponse> callback);

    class GetDiaryListRequest {
        public int pageNo;
        public int pageSize;
    }

    class CreateDiaryRequest {
        public String authorType;
        public String date;
        public String moodCode;
        public String moodText;
        public String content;
        public List<String> imageFileIds;
    }

    class DiaryListResponse {
        public List<DiarySummary> items;
        public boolean hasMore;
    }

    class DiarySummary {
        public String diaryId;
        public String date;
        public String authorType;
        public String moodText;
        public String contentSummary;
        public int imageCount;
        public String coverUrl;
    }

    class DiaryDetailResponse {
        public String diaryId;
        public String date;
        public String authorType;
        public String moodCode;
        public String moodText;
        public String content;
        public List<String> imageUrls;
    }

    class MoodTrendResponse {
        public List<MoodTrendPoint> points;
    }

    class MoodTrendPoint {
        public String date;
        public int score;
        public String moodText;
    }
}
