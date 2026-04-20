package com.nestchat.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nestchat.server.common.BusinessException;
import com.nestchat.server.common.IdGenerator;
import com.nestchat.server.common.ResultCode;
import com.nestchat.server.dto.request.SendImageMessageRequest;
import com.nestchat.server.dto.request.SendTextMessageRequest;
import com.nestchat.server.dto.request.SendVoiceMessageRequest;
import com.nestchat.server.dto.response.ChatSessionResponse;
import com.nestchat.server.dto.response.MessageListResponse;
import com.nestchat.server.dto.response.MessageResponse;
import com.nestchat.server.entity.Conversation;
import com.nestchat.server.entity.Diary;
import com.nestchat.server.entity.FileRecord;
import com.nestchat.server.entity.Message;
import com.nestchat.server.entity.Relation;
import com.nestchat.server.entity.User;
import com.nestchat.server.mapper.ConversationMapper;
import com.nestchat.server.mapper.DiaryMapper;
import com.nestchat.server.mapper.FileRecordMapper;
import com.nestchat.server.mapper.MessageMapper;
import com.nestchat.server.mapper.RelationMapper;
import com.nestchat.server.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final FileRecordMapper fileRecordMapper;
    private final RelationMapper relationMapper;
    private final DiaryMapper diaryMapper;

    public ChatService(ConversationMapper conversationMapper, MessageMapper messageMapper,
                       UserMapper userMapper, FileRecordMapper fileRecordMapper,
                       RelationMapper relationMapper, DiaryMapper diaryMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.fileRecordMapper = fileRecordMapper;
        this.relationMapper = relationMapper;
        this.diaryMapper = diaryMapper;
    }

    public ChatSessionResponse getCurrentSession(String userId) {
        Conversation conv = conversationMapper.selectByUserId(userId);
        if (conv == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "暂无会话");
        }

        String partnerId = conv.getUserIdA().equals(userId) ? conv.getUserIdB() : conv.getUserIdA();
        User partner = userMapper.selectById(partnerId);

        // Get relation info for companion days
        Relation relation = relationMapper.selectBoundByUserId(userId);
        int companionDays = 0;
        if (relation != null && relation.getBoundAt() != null) {
            companionDays = (int) ChronoUnit.DAYS.between(relation.getBoundAt().toLocalDate(), LocalDate.now());
        }

        // Check if partner has diary today
        boolean todayDiary = checkTodayDiary(partnerId);

        ChatSessionResponse resp = new ChatSessionResponse();
        resp.setConversationId(conv.getConversationId());
        resp.setPartnerUserId(partnerId);
        resp.setCompanionDays(companionDays);
        resp.setPartnerTodayDiary(todayDiary);

        if (partner != null) {
            resp.setPartnerNickname(partner.getNickname());
            resp.setPartnerAvatarUrl(partner.getAvatarUrl());
            resp.setPartnerMoodCode(partner.getMoodCode());
            resp.setPartnerMoodText(partner.getMoodText());
            if (partner.getLastActiveAt() != null) {
                resp.setPartnerLastActiveAt(formatDateTime(partner.getLastActiveAt()));
            }
        }
        resp.setSubtitle("已绑定 · 在线");
        return resp;
    }

    private boolean checkTodayDiary(String userId) {
        if (userId == null) return false;
        String today = LocalDate.now().format(DATE_FMT);
        Long count = diaryMapper.selectCount(
                new LambdaQueryWrapper<Diary>()
                        .eq(Diary::getUserId, userId)
                        .eq(Diary::getDate, today)
        );
        return count != null && count > 0;
    }

    public MessageListResponse getMessages(String userId, String conversationId, String cursor, int pageSize) {
        // 校验用户是会话参与者
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        if (!conv.getUserIdA().equals(userId) && !conv.getUserIdB().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        List<Message> messages = messageMapper.selectByCursor(conversationId, cursor, pageSize);

        List<MessageResponse> items = new ArrayList<>();
        for (Message msg : messages) {
            items.add(toMessageResponse(msg, userId));
        }

        MessageListResponse resp = new MessageListResponse();
        resp.setItems(items);
        resp.setHasMore(messages.size() == pageSize);
        resp.setNextCursor(messages.isEmpty() ? "" : messages.get(messages.size() - 1).getMessageId());
        return resp;
    }

    public MessageResponse sendTextMessage(String userId, SendTextMessageRequest req) {
        validateConversationAccess(userId, req.getConversationId());

        Message msg = new Message();
        msg.setMessageId(IdGenerator.timeBasedId("m"));
        msg.setConversationId(req.getConversationId());
        msg.setSenderUserId(userId);
        msg.setSenderType("");
        msg.setMessageType("text");
        msg.setContent(req.getContent());
        msg.setImageUrl("");
        msg.setVoiceUrl("");
        msg.setDurationSeconds(0);
        msg.setClientMessageId(req.getClientMessageId() != null ? req.getClientMessageId() : "");
        msg.setSendStatus("sent");
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);

        return toMessageResponse(msg, userId);
    }

    public MessageResponse sendImageMessage(String userId, SendImageMessageRequest req) {
        validateConversationAccess(userId, req.getConversationId());

        FileRecord file = fileRecordMapper.selectById(req.getImageFileId());
        if (file == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "图片文件不存在");
        }

        Message msg = new Message();
        msg.setMessageId(IdGenerator.timeBasedId("m"));
        msg.setConversationId(req.getConversationId());
        msg.setSenderUserId(userId);
        msg.setSenderType("");
        msg.setMessageType("image");
        msg.setContent("");
        msg.setImageUrl(file.getFileUrl());
        msg.setVoiceUrl("");
        msg.setDurationSeconds(0);
        msg.setClientMessageId(req.getClientMessageId() != null ? req.getClientMessageId() : "");
        msg.setSendStatus("sent");
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);

        return toMessageResponse(msg, userId);
    }

    public MessageResponse sendVoiceMessage(String userId, SendVoiceMessageRequest req) {
        validateConversationAccess(userId, req.getConversationId());

        FileRecord file = fileRecordMapper.selectById(req.getVoiceFileId());
        if (file == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "语音文件不存在");
        }

        Message msg = new Message();
        msg.setMessageId(IdGenerator.timeBasedId("m"));
        msg.setConversationId(req.getConversationId());
        msg.setSenderUserId(userId);
        msg.setSenderType("");
        msg.setMessageType("voice");
        msg.setContent("");
        msg.setImageUrl("");
        msg.setVoiceUrl(file.getFileUrl());
        msg.setDurationSeconds(req.getDurationSeconds() != null ? req.getDurationSeconds() : 0);
        msg.setClientMessageId(req.getClientMessageId() != null ? req.getClientMessageId() : "");
        msg.setSendStatus("sent");
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);

        return toMessageResponse(msg, userId);
    }

    private void validateConversationAccess(String userId, String conversationId) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        if (!conv.getUserIdA().equals(userId) && !conv.getUserIdB().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private MessageResponse toMessageResponse(Message msg, String currentUserId) {
        MessageResponse resp = new MessageResponse();
        resp.setMessageId(msg.getMessageId());
        resp.setConversationId(msg.getConversationId());
        resp.setSenderType(msg.getSenderUserId().equals(currentUserId) ? "me" : "ta");
        resp.setMessageType(msg.getMessageType());
        resp.setContent(msg.getContent());
        resp.setImageUrl(msg.getImageUrl());
        resp.setVoiceUrl(msg.getVoiceUrl());
        resp.setDurationSeconds(msg.getDurationSeconds());
        resp.setCreatedAt(msg.getCreatedAt().format(DT_FMT));
        resp.setClientMessageId(msg.getClientMessageId());
        resp.setSendStatus(msg.getSendStatus());
        return resp;
    }

    private String formatDateTime(LocalDateTime time) {
        return time == null ? null : time.format(DT_FMT);
    }
}
