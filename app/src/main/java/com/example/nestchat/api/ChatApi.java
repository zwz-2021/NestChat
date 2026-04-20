package com.example.nestchat.api;

import java.util.List;

/**
 * 聊天接口。
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
        public String partnerMoodCode;
        public String partnerMoodText;
        public String partnerLastActiveAt;
        public boolean partnerTodayDiary;
        public int companionDays;
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

    // ========== 实现 ==========
    class Impl {
        public static void getChatSession(ApiCallback<ChatSessionResponse> callback) {
            ApiClient.get("/chat/session/current", ChatSessionResponse.class, callback);
        }

        public static void getMessages(String conversationId, String cursor, int pageSize,
                                        ApiCallback<MessageListResponse> callback) {
            String path = "/chat/messages?conversationId=" + conversationId + "&pageSize=" + pageSize;
            if (cursor != null && !cursor.isEmpty()) {
                path += "&cursor=" + cursor;
            }
            ApiClient.get(path, MessageListResponse.class, callback);
        }

        public static void sendTextMessage(SendTextMessageRequest request, ApiCallback<MessageResponse> callback) {
            ApiClient.post("/chat/messages/text", request, MessageResponse.class, callback);
        }

        public static void sendImageMessage(SendImageMessageRequest request, ApiCallback<MessageResponse> callback) {
            ApiClient.post("/chat/messages/image", request, MessageResponse.class, callback);
        }

        public static void sendVoiceMessage(SendVoiceMessageRequest request, ApiCallback<MessageResponse> callback) {
            ApiClient.post("/chat/messages/voice", request, MessageResponse.class, callback);
        }
    }
}
