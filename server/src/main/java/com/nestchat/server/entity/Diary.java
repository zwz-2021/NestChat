package com.nestchat.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("diary")
public class Diary {

    @TableId(type = IdType.INPUT)
    private String diaryId;
    private String userId;
    private String date;
    private String moodCode;
    private String moodText;
    private String content;
    private LocalDateTime createdAt;
}
