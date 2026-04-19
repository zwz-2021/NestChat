package com.example.nestchat.api;

import java.io.File;

/**
 * 文件上传接口。
 */
public interface FileApi {

    void uploadImage(UploadFileRequest request, ApiCallback<UploadFileResponse> callback);
    void uploadVoice(UploadFileRequest request, ApiCallback<UploadFileResponse> callback);

    class UploadFileRequest {
        public String localPath;
        public String mimeType;
        public long fileSize;
        public String bizType;
    }

    class UploadFileResponse {
        public String fileId;
        public String fileUrl;
        public String thumbnailUrl;
        public String mimeType;
        public long fileSize;
        public int durationSeconds;
    }

    // ========== 实现 ==========
    class Impl {
        public static void uploadImage(UploadFileRequest request, ApiCallback<UploadFileResponse> callback) {
            File file = new File(request.localPath);
            String mime = request.mimeType != null ? request.mimeType : "image/jpeg";
            String biz = request.bizType != null ? request.bizType : "chat";
            ApiClient.uploadFile("/files/upload/image", file, "file", mime, biz,
                    UploadFileResponse.class, callback);
        }

        public static void uploadVoice(UploadFileRequest request, ApiCallback<UploadFileResponse> callback) {
            File file = new File(request.localPath);
            String mime = request.mimeType != null ? request.mimeType : "audio/mp4";
            String biz = request.bizType != null ? request.bizType : "chat";
            ApiClient.uploadFile("/files/upload/voice", file, "file", mime, biz,
                    UploadFileResponse.class, callback);
        }
    }
}
