package com.example.nestchat.api;

/**
 * 用户资料相关接口。
 */
public interface UserApi {

    void getMineProfile(ApiCallback<ProfileResponse> callback);
    void updateProfile(UpdateProfileRequest request, ApiCallback<ProfileResponse> callback);
    void updateMood(UpdateMoodRequest request, ApiCallback<ProfileResponse> callback);
    void heartbeat(ApiCallback<Void> callback);

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

    // ========== 实现 ==========
    class Impl {
        public static void getMineProfile(ApiCallback<ProfileResponse> callback) {
            ApiClient.get("/users/me", ProfileResponse.class, callback);
        }

        public static void updateProfile(UpdateProfileRequest request, ApiCallback<ProfileResponse> callback) {
            ApiClient.put("/users/me/profile", request, ProfileResponse.class, callback);
        }

        public static void updateMood(UpdateMoodRequest request, ApiCallback<ProfileResponse> callback) {
            ApiClient.put("/users/me/mood", request, ProfileResponse.class, callback);
        }

        public static void heartbeat(ApiCallback<Void> callback) {
            ApiClient.post("/users/me/heartbeat", null, Void.class, callback);
        }
    }
}
