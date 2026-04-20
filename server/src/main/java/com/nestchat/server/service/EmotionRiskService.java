package com.nestchat.server.service;

import com.nestchat.server.dto.response.EmotionRiskResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EmotionRiskService {

    private final AiService aiService;

    public EmotionRiskService(@Autowired(required = false) AiService aiService) {
        this.aiService = aiService;
    }

    public EmotionRiskResponse analyzePartnerTurn(List<String> partnerMessages) {
        List<String> normalizedMessages = normalizeMessages(partnerMessages);
        if (normalizedMessages.isEmpty()) {
            return none();
        }

        if (aiService != null) {
            EmotionRiskResponse aiResult = aiService.analyzeEmotionRisk(normalizedMessages);
            EmotionRiskResponse sanitized = sanitize(aiResult);
            if (sanitized != null) {
                return sanitized;
            }
        }

        return analyzeByRules(normalizedMessages);
    }

    private List<String> normalizeMessages(List<String> partnerMessages) {
        List<String> result = new ArrayList<>();
        if (partnerMessages == null) {
            return result;
        }
        for (String message : partnerMessages) {
            String trimmed = safeTrim(message);
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private EmotionRiskResponse analyzeByRules(List<String> partnerMessages) {
        String text = String.join("\n", partnerMessages);
        String compactText = text.replace(" ", "");

        int highRiskScore = countMatches(compactText, List.of(
                "崩溃", "撑不住", "受不了了", "活着没意思", "不想活", "不想说了", "别逼我"
        ));
        if (highRiskScore > 0) {
            return buildPrompt("high", "overwhelmed",
                    "情绪风险提示",
                    "TA 刚刚这段话情绪波动比较大，建议先安抚和确认感受，暂时不要争辩或连续追问。");
        }

        Map<String, List<String>> emotionKeywords = new LinkedHashMap<>();
        emotionKeywords.put("angry", List.of("生气", "烦", "烦死", "别烦", "讨厌", "气死", "火大", "受不了"));
        emotionKeywords.put("wronged", List.of("委屈", "凭什么", "算了", "无所谓", "没必要", "不公平"));
        emotionKeywords.put("sad", List.of("难过", "伤心", "失望", "想哭", "哭了", "心累", "不开心"));
        emotionKeywords.put("anxious", List.of("焦虑", "害怕", "担心", "怎么办", "压力", "慌", "睡不着"));
        emotionKeywords.put("tired", List.of("好累", "很累", "累死", "疲惫", "没力气", "不想说话"));

        String bestEmotion = "";
        int bestScore = 0;
        for (Map.Entry<String, List<String>> entry : emotionKeywords.entrySet()) {
            int score = countMatches(compactText, entry.getValue());
            if (score > bestScore) {
                bestEmotion = entry.getKey();
                bestScore = score;
            }
        }

        if (bestScore <= 0) {
            return none();
        }

        return switch (bestEmotion) {
            case "angry" -> buildPrompt("medium", "angry",
                    "情绪风险提示",
                    "TA 刚刚这段话里带有明显烦躁或生气情绪，建议先接住情绪，少解释、少反问。");
            case "wronged" -> buildPrompt("medium", "wronged",
                    "情绪风险提示",
                    "TA 这段表达里可能带着委屈感，建议先回应感受，再进入事实讨论。");
            case "sad" -> buildPrompt("medium", "sad",
                    "情绪风险提示",
                    "TA 这段话里有低落情绪，建议先安抚和陪伴，不要急着给结论。");
            case "anxious" -> buildPrompt("medium", "anxious",
                    "情绪风险提示",
                    "TA 可能正处于焦虑状态，建议先给确定感和回应，不要连续追问。");
            case "tired" -> buildPrompt("medium", "tired",
                    "情绪风险提示",
                    "TA 看起来有些疲惫或抗拒继续沟通，建议先放缓节奏，降低话题强度。");
            default -> none();
        };
    }

    private EmotionRiskResponse sanitize(EmotionRiskResponse response) {
        if (response == null) {
            return null;
        }

        String riskLevel = safeTrim(response.getRiskLevel()).toLowerCase(Locale.ROOT);
        if (!List.of("none", "medium", "high").contains(riskLevel)) {
            riskLevel = response.isShouldPrompt() ? "medium" : "none";
        }

        boolean shouldPrompt = response.isShouldPrompt()
                && !"none".equals(riskLevel)
                && !safeTrim(response.getMessage()).isEmpty();

        EmotionRiskResponse sanitized = new EmotionRiskResponse();
        sanitized.setShouldPrompt(shouldPrompt);
        sanitized.setRiskLevel(shouldPrompt ? riskLevel : "none");
        sanitized.setEmotionTag(safeTrim(response.getEmotionTag()));
        sanitized.setTitle(safeTrim(response.getTitle()).isEmpty() ? "情绪风险提示" : safeTrim(response.getTitle()));
        sanitized.setMessage(shouldPrompt ? safeTrim(response.getMessage()) : "");
        return sanitized;
    }

    private int countMatches(String text, List<String> keywords) {
        int score = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                score++;
            }
        }
        return score;
    }

    private EmotionRiskResponse buildPrompt(String riskLevel, String emotionTag, String title, String message) {
        EmotionRiskResponse response = new EmotionRiskResponse();
        response.setShouldPrompt(true);
        response.setRiskLevel(riskLevel);
        response.setEmotionTag(emotionTag);
        response.setTitle(title);
        response.setMessage(message);
        return response;
    }

    private EmotionRiskResponse none() {
        EmotionRiskResponse response = new EmotionRiskResponse();
        response.setShouldPrompt(false);
        response.setRiskLevel("none");
        response.setEmotionTag("");
        response.setTitle("");
        response.setMessage("");
        return response;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
