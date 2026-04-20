package com.nestchat.server.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class AnalyzeEmotionRiskRequest {

    private List<String> partnerMessages;
}
