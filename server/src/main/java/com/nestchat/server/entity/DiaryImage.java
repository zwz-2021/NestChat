package com.nestchat.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("diary_image")
public class DiaryImage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String diaryId;
    private String imageUrl;
    private Integer sortOrder;
}
