package com.example.nestchat.api;

import java.util.List;

/**
 * 聊天接口占位。
 */
public interface ChatApi {

    void getChatSession(ApiCallback<ChatSessionResponse> callback);

    void getMessages(GetMessagesRequest request, ApiCallback<MessageListResponse> callback);

    void sendTextMessage(SendTextMessageRequest request, ApiCallback<MessageResponse> callback);

    void sendImageMessage(SendImageMessageRequest request, ApiCallback<MessageResponse> callback);

    void sendVoiceMessage(SendVoiceMessageRequest request, ApiCallback<MessageResponse> callback);

    class GetMessagesRequest {
        public String conversationId;
        public String cursor;
        public int pageSize;
    }

    class SendTextMessageRequest {
        public String conversationId;
        public String content;
        public String clientMessageId;
    }

    class SendImageMessageRequest {
        public String conversationId;
        public String imageFileId;
        public String clientMessageId;
    }

    class SendVoiceMessageRequest {
        public String conversationId;
        public String voiceFileId;
        public int durationSeconds;
        public String clientMessageId;
    }

    class ChatSessionResponse {
        public String conversationId;
        public String partnerUserId;
        public String partnerNickname;
        public String partnerAvatarUrl;
        public String subtitle;
    }

    class MessageListResponse {
        public List<MessageResponse> items;
        public String nextCursor;
        public boolean hasMore;
    }

    class MessageResponse {
        public String messageId;
        public String conversationId;
        public String senderType;
        public String messageType;
        public String content;
        public String imageUrl;
        public String voiceUrl;
        public int durationSeconds;
        public String createdAt;
        public String clientMessageId;
        public String sendStatus;
    }
}
