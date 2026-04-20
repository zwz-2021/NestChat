package com.nestchat.server.controller;

import com.nestchat.server.common.Result;
import com.nestchat.server.dto.request.OptimizeMessageRequest;
import com.nestchat.server.dto.request.SendImageMessageRequest;
import com.nestchat.server.dto.request.SendTextMessageRequest;
import com.nestchat.server.dto.request.SendVoiceMessageRequest;
import com.nestchat.server.dto.response.ChatSessionResponse;
import com.nestchat.server.dto.response.MessageListResponse;
import com.nestchat.server.dto.response.MessageResponse;
import com.nestchat.server.dto.response.OptimizeMessageResponse;
import com.nestchat.server.security.UserContext;
import com.nestchat.server.service.AiService;
import com.nestchat.server.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final AiService aiService;

    public ChatController(ChatService chatService, @Autowired(required = false) AiService aiService) {
        this.chatService = chatService;
        this.aiService = aiService;
    }

    @GetMapping("/session/current")
    public Result<ChatSessionResponse> getCurrentSession() {
        return Result.ok(chatService.getCurrentSession(UserContext.get()));
    }

    @GetMapping("/messages")
    public Result<MessageListResponse> getMessages(@RequestParam String conversationId,
                                                    @RequestParam(required = false) String cursor,
                                                    @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(chatService.getMessages(UserContext.get(), conversationId, cursor, pageSize));
    }

    @PostMapping("/messages/text")
    public Result<MessageResponse> sendTextMessage(@Valid @RequestBody SendTextMessageRequest req) {
        return Result.ok(chatService.sendTextMessage(UserContext.get(), req));
    }

    @PostMapping("/messages/image")
    public Result<MessageResponse> sendImageMessage(@Valid @RequestBody SendImageMessageRequest req) {
        return Result.ok(chatService.sendImageMessage(UserContext.get(), req));
    }

    @PostMapping("/messages/voice")
    public Result<MessageResponse> sendVoiceMessage(@Valid @RequestBody SendVoiceMessageRequest req) {
        return Result.ok(chatService.sendVoiceMessage(UserContext.get(), req));
    }

    @PostMapping("/optimize")
    public Result<OptimizeMessageResponse> optimizeMessage(@Valid @RequestBody OptimizeMessageRequest req) {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatController.class);

        if (aiService == null) {
            log.warn("AiService 未注入，返回 fallback 结果");
            OptimizeMessageResponse fallback = new OptimizeMessageResponse();
            fallback.setOptimizedText(req.getOriginalText());
            fallback.setMode(req.getMode());
            return Result.ok(fallback);
        }

        log.info("调用 AiService 优化消息");
        String optimized = aiService.optimizeMessage(req.getOriginalText(), req.getMode());

        OptimizeMessageResponse response = new OptimizeMessageResponse();
        response.setOptimizedText(optimized);
        response.setMode(req.getMode());
        return Result.ok(response);
    }
}
