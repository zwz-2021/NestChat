package com.example.nestchat.api;

import java.util.List;

/**
 * 关系管理接口。
 */
public interface RelationApi {

    void getCurrentRelation(ApiCallback<RelationStatusResponse> callback);
    void createBindRequest(CreateBindRequest request, ApiCallback<RelationStatusResponse> callback);
    void getRelationApplications(ApiCallback<RelationApplicationsResponse> callback);
    void acceptRelation(String applicationId, ApiCallback<RelationStatusResponse> callback);
    void rejectRelation(String applicationId, ApiCallback<RelationStatusResponse> callback);
    void updateRemark(UpdateRemarkRequest request, ApiCallback<RelationStatusResponse> callback);
    void unbind(ApiCallback<SimpleResponse> callback);

    class CreateBindRequest {
        public String targetPhone;
    }

    class UpdateRemarkRequest {
        public String relationId;
        public String remark;
    }

    class RelationStatusResponse {
        public String relationId;
        public String status;
        public String partnerUserId;
        public String partnerPhone;
        public String partnerNickname;
        public String partnerAvatarUrl;
        public String partnerRemark;
        public String boundAt;
        public int companionDays;
    }

    class RelationApplicationsResponse {
        public List<RelationApplication> items;
    }

    class RelationApplication {
        public String applicationId;
        public String status;
        public String initiatorUserId;
        public String initiatorPhone;
        public String targetUserId;
        public String targetPhone;
        public String createdAt;
    }

    class SimpleResponse {
        public boolean success;
        public String message;
    }

    // ========== 实现 ==========
    class Impl {
        public static void getCurrentRelation(ApiCallback<RelationStatusResponse> callback) {
            ApiClient.get("/relations/current", RelationStatusResponse.class, callback);
        }

        public static void createBindRequest(CreateBindRequest request, ApiCallback<RelationStatusResponse> callback) {
            ApiClient.post("/relations/applications", request, RelationStatusResponse.class, callback);
        }

        public static void getRelationApplications(ApiCallback<RelationApplicationsResponse> callback) {
            ApiClient.get("/relations/applications", RelationApplicationsResponse.class, callback);
        }

        public static void acceptRelation(String applicationId, ApiCallback<RelationStatusResponse> callback) {
            ApiClient.post("/relations/applications/" + applicationId + "/accept", null,
                    RelationStatusResponse.class, callback);
        }

        public static void rejectRelation(String applicationId, ApiCallback<RelationStatusResponse> callback) {
            ApiClient.post("/relations/applications/" + applicationId + "/reject", null,
                    RelationStatusResponse.class, callback);
        }

        public static void updateRemark(UpdateRemarkRequest request, ApiCallback<RelationStatusResponse> callback) {
            ApiClient.put("/relations/current/remark", request, RelationStatusResponse.class, callback);
        }

        public static void unbind(ApiCallback<SimpleResponse> callback) {
            ApiClient.delete("/relations/current", SimpleResponse.class, callback);
        }
    }
}
