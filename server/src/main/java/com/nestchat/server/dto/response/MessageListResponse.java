package com.nestchat.server.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class MessageListResponse {

    private List<MessageResponse> items;
    private String nextCursor;
    private Boolean hasMore;
}
