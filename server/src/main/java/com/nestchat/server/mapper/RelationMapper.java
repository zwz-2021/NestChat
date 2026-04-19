package com.nestchat.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nestchat.server.entity.Relation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface RelationMapper extends BaseMapper<Relation> {

    @Select("SELECT * FROM relation WHERE (user_id_a = #{userId} OR user_id_b = #{userId}) AND status = 'bound' LIMIT 1")
    Relation selectBoundByUserId(@Param("userId") String userId);
}
