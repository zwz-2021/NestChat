package com.nestchat.server.dto.response;

import lombok.Data;

@Data
public class UploadFileResponse {

    private String fileId;
    private String fileUrl;
    private String thumbnailUrl;
    private String mimeType;
    private Long fileSize;
    private Integer durationSeconds;
}
