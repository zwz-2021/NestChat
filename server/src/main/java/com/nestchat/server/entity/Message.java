package com.nestchat.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.INPUT)
    private String messageId;
    private String conversationId;
    private String senderUserId;
    private String senderType;
    private String messageType;
    private String content;
    private String imageUrl;
    private String voiceUrl;
    private Integer durationSeconds;
    private String clientMessageId;
    private String sendStatus;
    private LocalDateTime createdAt;
}
