package com.nestchat.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_record")
public class FileRecord {

    @TableId(type = IdType.INPUT)
    private String fileId;
    private String userId;
    private String fileUrl;
    private String thumbnailUrl;
    private String mimeType;
    private Long fileSize;
    private String bizType;
    private Integer durationSeconds;
    private LocalDateTime createdAt;
}
