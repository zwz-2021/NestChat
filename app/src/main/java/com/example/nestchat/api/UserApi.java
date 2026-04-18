package com.example.nestchat.api;

/**
 * 用户资料相关接口占位。
 */
public interface UserApi {

    void getMineProfile(ApiCallback<ProfileResponse> callback);

    void updateProfile(UpdateProfileRequest request, ApiCallback<ProfileResponse> callback);

    void updateMood(UpdateMoodRequest request, ApiCallback<ProfileResponse> callback);

    class ProfileResponse {
        public String userId;
        public String account;
        public String nickname;
        public String avatarUrl;
        public String moodCode;
        public String moodText;
    }

    class UpdateProfileRequest {
        public String nickname;
        public String avatarUrl;
    }

    class UpdateMoodRequest {
        public String moodCode;
    }
}
