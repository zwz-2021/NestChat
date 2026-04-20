package com.nestchat.server.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nestchat.server.dto.response.DiaryInsightResponse;
import com.nestchat.server.dto.response.EmotionRiskResponse;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "nestchat.ai.enabled", havingValue = "true", matchIfMissing = false)
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public AiService(@Value("${nestchat.ai.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        log.info("AiService 已启用，API Key: {}...", apiKey != null ? apiKey.substring(0, 10) : "null");
    }

    public String optimizeMessage(String originalText, String mode) {
        log.info("AI 优化请求 - 原话: {}, 模式: {}", originalText, mode);
        String systemPrompt = buildSystemPrompt(mode);
        String userPrompt = buildUserPrompt(originalText, mode);

        try {
            ChatRequest request = new ChatRequest();
            request.model = "qwen-turbo";

            ChatMessage systemMsg = new ChatMessage();
            systemMsg.role = "system";
            systemMsg.content = systemPrompt;

            ChatMessage userMsg = new ChatMessage();
            userMsg.role = "user";
            userMsg.content = userPrompt;

            request.messages = List.of(systemMsg, userMsg);
            request.temperature = 0.7;
            request.maxTokens = 200;

            String jsonBody = objectMapper.writeValueAsString(request);

            Request httpRequest = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    log.error("AI API 调用失败: {}", response.code());
                    return getFallbackResult(mode, originalText);
                }

                String responseBody = response.body() != null ? response.body().string() : null;
                if (responseBody == null) {
                    log.error("AI API 响应为空");
                    return getFallbackResult(mode, originalText);
                }

                log.debug("AI API 原始响应: {}", responseBody);

                JsonNode jsonNode = objectMapper.readTree(responseBody);
                String content = jsonNode.path("choices")
                        .path(0)
                        .path("message")
                        .path("content")
                        .asText();

                if (content != null && !content.isEmpty()) {
                    log.info("AI 优化成功: {} -> {}", originalText, content);
                    return content.trim();
                }

                log.error("AI API 响应解析失败，content 为空");
                return getFallbackResult(mode, originalText);
            }
        } catch (IOException e) {
            log.error("AI 服务调用异常", e);
            return getFallbackResult(mode, originalText);
        }
    }

    public EmotionRiskResponse analyzeEmotionRisk(List<String> partnerMessages) {
        List<String> normalizedMessages = new ArrayList<>();
        for (String message : partnerMessages) {
            if (message != null && !message.trim().isEmpty()) {
                normalizedMessages.add(message.trim());
            }
        }
        if (normalizedMessages.isEmpty()) {
            return null;
        }

        try {
            ChatRequest request = new ChatRequest();
            request.model = "qwen-turbo";

            ChatMessage systemMsg = new ChatMessage();
            systemMsg.role = "system";
            systemMsg.content = """
                    你是一名亲密关系聊天中的情绪风险识别助手。
                    你只能分析用户提供的“对方刚刚这一段连续发言”，不能引用、更不能推断更早的聊天记录。
                    你的任务是判断这段发言是否存在需要在聊天页提示用户注意的情绪风险。
                    只有在明显的低落、委屈、焦虑、烦躁、生气、崩溃感等情况下才提示。
                    如果没有明显风险，就返回 shouldPrompt=false。
                    只返回严格 JSON，不要输出 markdown，不要解释。
                    JSON 字段固定为：
                    shouldPrompt: boolean
                    riskLevel: none | medium | high
                    emotionTag: sad | wronged | anxious | angry | tired | overwhelmed | none
                    title: string
                    message: string
                    """;

            ChatMessage userMsg = new ChatMessage();
            userMsg.role = "user";
            userMsg.content = "只分析下面这段刚刚的新发言：\n" + String.join("\n", normalizedMessages);

            request.messages = List.of(systemMsg, userMsg);
            request.temperature = 0.2;
            request.maxTokens = 180;

            String jsonBody = objectMapper.writeValueAsString(request);
            Request httpRequest = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    log.error("AI 情绪风险识别调用失败: {}", response.code());
                    return null;
                }

                String responseBody = response.body() != null ? response.body().string() : null;
                if (responseBody == null || responseBody.isEmpty()) {
                    log.error("AI 情绪风险识别响应为空");
                    return null;
                }

                JsonNode jsonNode = objectMapper.readTree(responseBody);
                String content = jsonNode.path("choices")
                        .path(0)
                        .path("message")
                        .path("content")
                        .asText();
                if (content == null || content.isBlank()) {
                    return null;
                }

                return objectMapper.readValue(content.trim(), EmotionRiskResponse.class);
            }
        } catch (Exception e) {
            log.error("AI 情绪风险识别异常", e);
            return null;
        }
    }

    public DiaryInsightResponse summarizeDiary(String moodCode, String moodText, String content, int imageCount) {
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isEmpty()) {
            return null;
        }

        try {
            ChatRequest request = new ChatRequest();
            request.model = "qwen-turbo";

            ChatMessage systemMsg = new ChatMessage();
            systemMsg.role = "system";
            systemMsg.content = """
                    你是一个帮助用户做日记总结的助手。
                    你只能根据用户提供的 moodCode、moodText、日记正文和图片数量生成结果，不能虚构新的事实。
                    请输出严格 JSON，不要输出 markdown，不要解释。
                    JSON 字段固定为：
                    emotionSummary: string
                    triggerEvent: string
                    messageToPartner: string
                    要求：
                    1. 三个字段都用简洁自然的中文，每个字段控制在 18 到 50 个字。
                    2. emotionSummary 总结今天的整体情绪。
                    3. triggerEvent 提炼引发情绪的事件或场景；如果正文不够明确，就写“今天最在意的事情还需要再说得更具体一点”这种保守表达。
                    4. messageToPartner 用第一人称，像用户想对 TA 说的一句话，语气自然，不要肉麻，不要夸张。
                    5. 不要重复字段名，不要多输出其他内容。
                    """;

            ChatMessage userMsg = new ChatMessage();
            userMsg.role = "user";
            userMsg.content = "moodCode=" + safeValue(moodCode)
                    + "\nmoodText=" + safeValue(moodText)
                    + "\nimageCount=" + imageCount
                    + "\ncontent=\n" + normalizedContent;

            request.messages = List.of(systemMsg, userMsg);
            request.temperature = 0.4;
            request.maxTokens = 220;

            String jsonBody = objectMapper.writeValueAsString(request);
            Request httpRequest = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    log.error("AI 日记总结调用失败: {}", response.code());
                    return null;
                }

                String responseBody = response.body() != null ? response.body().string() : null;
                if (responseBody == null || responseBody.isEmpty()) {
                    log.error("AI 日记总结响应为空");
                    return null;
                }

                JsonNode jsonNode = objectMapper.readTree(responseBody);
                String result = jsonNode.path("choices")
                        .path(0)
                        .path("message")
                        .path("content")
                        .asText();
                if (result == null || result.isBlank()) {
                    return null;
                }

                return objectMapper.readValue(result.trim(), DiaryInsightResponse.class);
            }
        } catch (Exception e) {
            log.error("AI 日记总结异常", e);
            return null;
        }
    }

    private String buildSystemPrompt(String mode) {
        return switch (mode) {
            case "gentle" -> "你是一位专业的情感沟通顾问。你的任务是将用户的话语表达得更温柔、更柔和，降低攻击性，同时保留原意。只在亲密关系中使用，语气要自然、不生硬。直接返回优化后的话语，不要任何解释或额外说明。";
            case "sincere" -> "你是一位专业的情感沟通顾问。你的任务是将用户的话语表达得更真诚、更有情感，让对方感受到真心。只在亲密关系中使用，语气要自然、不做作。直接返回优化后的话语，不要任何解释或额外说明。";
            case "comfort" -> "你是一位专业的情感沟通顾问。你的任务是帮用户组织一句安慰对方的话，适合在对方情绪不好时使用。要温暖、体贴，给人安全感。直接返回优化后的话语，不要任何解释或额外说明。";
            default -> "你是一位专业的情感沟通顾问。直接返回优化后的话语，不要任何解释或额外说明。";
        };
    }

    private String buildUserPrompt(String originalText, String mode) {
        return switch (mode) {
            case "gentle" -> "请把这句话说得更温柔一点：" + originalText;
            case "sincere" -> "请把这句话说得更真诚、更有情感一点：" + originalText;
            case "comfort" -> "对方现在可能情绪不好，请帮我说一句安慰的话，想表达的意思是：" + originalText;
            default -> "请优化这句话：" + originalText;
        };
    }

    private String getFallbackResult(String mode, String originalText) {
        return switch (mode) {
            case "gentle" -> "我想轻轻地告诉你，" + originalText;
            case "sincere" -> "真心地想对你说，" + originalText;
            case "comfort" -> "如果现在心情不好，我想告诉你，" + originalText;
            default -> originalText;
        };
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private static class ChatRequest {
        public String model;
        public List<ChatMessage> messages;
        @JsonProperty("temperature")
        public double temperature;
        @JsonProperty("max_tokens")
        public int maxTokens;
    }

    private static class ChatMessage {
        public String role;
        public String content;
    }
}
