package com.nestchat.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.INPUT)
    private String userId;
    private String account;
    private String password;
    private String nickname;
    private String avatarUrl;
    private String moodCode;
    private String moodText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
