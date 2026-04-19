package com.nestchat.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("relation")
public class Relation {

    @TableId(type = IdType.INPUT)
    private String relationId;
    private String userIdA;
    private String userIdB;
    private String remarkA;
    private String remarkB;
    private String status;
    private LocalDateTime boundAt;
    private LocalDateTime createdAt;
}
