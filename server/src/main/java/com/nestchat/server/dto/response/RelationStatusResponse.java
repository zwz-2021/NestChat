package com.nestchat.server.dto.response;

import lombok.Data;

@Data
public class RelationStatusResponse {

    private String relationId;
    private String status;
    private String pendingApplicationId;
    private String pendingApplicationType;
    private String pendingApplicationRole;
    private String pendingApplicationCreatedAt;
    private String partnerUserId;
    private String partnerPhone;
    private String partnerNickname;
    private String partnerAvatarUrl;
    private String partnerRemark;
    private String boundAt;
    private Long companionDays;
}
