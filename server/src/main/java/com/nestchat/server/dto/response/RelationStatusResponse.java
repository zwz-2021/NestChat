package com.nestchat.server.dto.response;

import lombok.Data;

@Data
public class RelationStatusResponse {

    private String relationId;
    private String status;
    private String partnerUserId;
    private String partnerPhone;
    private String partnerNickname;
    private String partnerAvatarUrl;
    private String partnerRemark;
    private String boundAt;
    private Long companionDays;
}
