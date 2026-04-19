package com.nestchat.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nestchat.server.entity.Message;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MessageMapper extends BaseMapper<Message> {

    @Select("<script>" +
            "SELECT * FROM message WHERE conversation_id = #{conversationId} " +
            "<if test='cursor != null and cursor != \"\"'> AND message_id &lt; #{cursor} </if>" +
            "ORDER BY message_id DESC LIMIT #{pageSize}" +
            "</script>")
    List<Message> selectByCursor(@Param("conversationId") String conversationId,
                                 @Param("cursor") String cursor,
                                 @Param("pageSize") int pageSize);
}
