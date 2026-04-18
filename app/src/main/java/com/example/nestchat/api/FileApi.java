package com.example.nestchat.api;

/**
 * 文件上传接口占位。
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
}
