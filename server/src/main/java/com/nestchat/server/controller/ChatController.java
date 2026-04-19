package com.nestchat.server.controller;

import com.nestchat.server.common.Result;
import com.nestchat.server.dto.request.SendImageMessageRequest;
import com.nestchat.server.dto.request.SendTextMessageRequest;
import com.nestchat.server.dto.request.SendVoiceMessageRequest;
import com.nestchat.server.dto.response.ChatSessionResponse;
import com.nestchat.server.dto.response.MessageListResponse;
import com.nestchat.server.dto.response.MessageResponse;
import com.nestchat.server.security.UserContext;
import com.nestchat.server.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
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
}
