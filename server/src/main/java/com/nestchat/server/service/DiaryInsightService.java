package com.nestchat.server.service;

import com.nestchat.server.dto.response.DiaryInsightResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiaryInsightService {

    private final AiService aiService;

    public DiaryInsightService(@Autowired(required = false) AiService aiService) {
        this.aiService = aiService;
    }

    public DiaryInsightResponse summarize(String moodCode, String moodText, String content, List<String> imageUrls) {
        DiaryInsightResponse aiResult = null;
        if (aiService != null) {
            aiResult = aiService.summarizeDiary(moodCode, moodText, content, imageUrls == null ? 0 : imageUrls.size());
        }
        if (isComplete(aiResult)) {
            return trim(aiResult);
        }
        return buildFallback(moodCode, moodText, content, imageUrls == null ? 0 : imageUrls.size());
    }

    private DiaryInsightResponse buildFallback(String moodCode, String moodText, String content, int imageCount) {
        String normalizedMoodCode = safeTrim(moodCode);
        String normalizedMoodText = safeTrim(moodText);
        String normalizedContent = safeTrim(content);
        String excerpt = firstSentence(normalizedContent, 34);
        String imageHint = imageCount > 0 ? "，还专门配了" + imageCount + "张图片" : "";

        DiaryInsightResponse response = new DiaryInsightResponse();
        response.setEmotionSummary(buildEmotionSummary(normalizedMoodCode, normalizedMoodText, normalizedContent, imageHint));
        response.setTriggerEvent(buildTriggerEvent(normalizedMoodCode, excerpt, imageHint));
        response.setMessageToPartner(buildMessageToPartner(normalizedMoodCode, normalizedMoodText, excerpt));
        return trim(response);
    }

    private String buildEmotionSummary(String moodCode, String moodText, String content, String imageHint) {
        String label = moodText.isEmpty() ? "今天的情绪" : "今天整体是" + moodText + "的心情";
        return switch (moodCode) {
            case "happy", "love" -> label + "，字里行间能看出开心和在意" + imageHint;
            case "calm" -> label + "，更像是在慢慢整理今天发生的事" + imageHint;
            case "sad" -> label + "，能感觉到失落和需要被接住的那一面";
            case "wronged" -> label + "，委屈感比较明显，心里还有些话没真正说开";
            case "angry" -> label + "，情绪被明显带起来了，背后其实也夹着在意";
            case "tired" -> label + "，状态有些累，像是在硬撑着消化今天的事";
            case "anxious" -> label + "，心里有点悬着，说明这件事对你影响不小";
            default -> {
                if (content.length() > 28) {
                    yield "今天的情绪都落在这段记录里了，能看出你很在意发生的这些事";
                }
                yield "今天的情绪已经被认真记录下来了，整体像是在整理当下真实感受";
            }
        };
    }

    private String buildTriggerEvent(String moodCode, String excerpt, String imageHint) {
        if (excerpt.isEmpty()) {
            return "今天最在意的事情还需要再说得更具体一点，情绪来源暂时只看得出是日常相处";
        }
        return switch (moodCode) {
            case "happy", "love" -> "让你情绪起伏的重点，多半就是这件事：" + excerpt + imageHint;
            case "sad", "wronged", "angry", "anxious" -> "这次情绪被带起来，核心还是因为：" + excerpt + imageHint;
            case "tired" -> "让你觉得有点消耗的，主要还是这件事：" + excerpt + imageHint;
            default -> "今天最牵动情绪的片段，大概率就是：" + excerpt + imageHint;
        };
    }

    private String buildMessageToPartner(String moodCode, String moodText, String excerpt) {
        return switch (moodCode) {
            case "happy", "love" -> "今天这件事让我挺开心的，我想把这种在意也认真告诉你。";
            case "calm" -> "今天的感受我已经慢慢整理好了，也想找个合适的时候和你说说。";
            case "sad" -> "我今天其实有点难过，如果你愿意的话，我想慢慢和你讲。";
            case "wronged" -> "我心里还是有点委屈，希望你能先听我把感受说完。";
            case "angry" -> "我不是只想发脾气，我更想让你知道我为什么会在意这件事。";
            case "tired" -> "我今天真的有点累了，希望你能多给我一点理解和缓冲。";
            case "anxious" -> "这件事让我一直有点不安，我想听听你的想法，也想被你安稳一下。";
            default -> {
                if (!excerpt.isEmpty()) {
                    yield "关于“" + excerpt + "”这件事，我其实还有些感受想认真告诉你。";
                }
                if (!moodText.isEmpty()) {
                    yield "我今天是" + moodText + "的状态，也想让你更懂我一点。";
                }
                yield "我今天有些感受还没完全说出来，想找个合适的时候认真和你聊聊。";
            }
        };
    }

    private boolean isComplete(DiaryInsightResponse response) {
        return response != null
                && !safeTrim(response.getEmotionSummary()).isEmpty()
                && !safeTrim(response.getTriggerEvent()).isEmpty()
                && !safeTrim(response.getMessageToPartner()).isEmpty();
    }

    private DiaryInsightResponse trim(DiaryInsightResponse response) {
        DiaryInsightResponse trimmed = new DiaryInsightResponse();
        trimmed.setEmotionSummary(limit(safeTrim(response.getEmotionSummary()), 56));
        trimmed.setTriggerEvent(limit(safeTrim(response.getTriggerEvent()), 60));
        trimmed.setMessageToPartner(limit(safeTrim(response.getMessageToPartner()), 60));
        return trimmed;
    }

    private String firstSentence(String content, int maxLen) {
        if (content.isEmpty()) {
            return "";
        }
        String normalized = content.replace("\r", " ").replace("\n", " ").trim();
        int cut = normalized.length();
        for (String separator : List.of("。", "！", "？", ".", "!", "?", "；", ";")) {
            int index = normalized.indexOf(separator);
            if (index >= 0) {
                cut = Math.min(cut, index);
            }
        }
        String value = normalized.substring(0, Math.min(cut, normalized.length())).trim();
        return limit(value.isEmpty() ? normalized : value, maxLen);
    }

    private String limit(String value, int maxLen) {
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen - 1) + "…";
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
