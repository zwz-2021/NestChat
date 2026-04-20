package com.nestchat.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nestchat.server.entity.Conversation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ConversationMapper extends BaseMapper<Conversation> {

    @Select("SELECT * FROM conversation WHERE (user_id_a = #{userId} OR user_id_b = #{userId}) LIMIT 1")
    Conversation selectByUserId(@Param("userId") String userId);

    @Select("SELECT * FROM conversation WHERE " +
            "(user_id_a = #{userIdA} AND user_id_b = #{userIdB}) OR " +
            "(user_id_a = #{userIdB} AND user_id_b = #{userIdA}) LIMIT 1")
    Conversation selectByUserIds(@Param("userIdA") String userIdA, @Param("userIdB") String userIdB);
}
