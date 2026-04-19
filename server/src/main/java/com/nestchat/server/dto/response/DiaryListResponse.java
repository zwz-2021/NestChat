package com.nestchat.server.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class DiaryListResponse {

    private List<DiarySummaryResponse> items;
    private Boolean hasMore;
}
