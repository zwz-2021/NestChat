package com.nestchat.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("relation_application")
public class RelationApplication {

    @TableId(type = IdType.INPUT)
    private String applicationId;
    private String initiatorUserId;
    private String targetUserId;
    private String status;
    private LocalDateTime createdAt;
}
