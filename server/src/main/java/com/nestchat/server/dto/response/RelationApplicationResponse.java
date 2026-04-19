package com.nestchat.server.dto.response;

import lombok.Data;

@Data
public class RelationApplicationResponse {

    private String applicationId;
    private String status;
    private String initiatorUserId;
    private String initiatorPhone;
    private String targetUserId;
    private String targetPhone;
    private String createdAt;
}
