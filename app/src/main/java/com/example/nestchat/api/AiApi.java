package com.example.nestchat.api;

/**
 * AI 表达辅助接口。
 */
public interface AiApi {

    void optimizeMessage(OptimizeMessageRequest request, ApiCallback<OptimizeMessageResponse> callback);

    class OptimizeMessageRequest {
        public String originalText;
        public String mode; // gentle, sincere, comfort
    }

    class OptimizeMessageResponse {
        public String optimizedText;
        public String mode;
    }

    // ========== 实现 ==========
    class Impl {
        public static void optimizeMessage(String originalText, String mode,
                                           ApiCallback<OptimizeMessageResponse> callback) {
            OptimizeMessageRequest request = new OptimizeMessageRequest();
            request.originalText = originalText;
            request.mode = mode;
            ApiClient.post("/chat/optimize", request, OptimizeMessageResponse.class, callback);
        }
    }
}