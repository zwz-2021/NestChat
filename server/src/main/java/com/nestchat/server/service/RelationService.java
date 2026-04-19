package com.nestchat.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nestchat.server.common.BusinessException;
import com.nestchat.server.common.IdGenerator;
import com.nestchat.server.common.ResultCode;
import com.nestchat.server.dto.request.CreateBindRequest;
import com.nestchat.server.dto.request.UpdateRemarkRequest;
import com.nestchat.server.dto.response.RelationApplicationResponse;
import com.nestchat.server.dto.response.RelationStatusResponse;
import com.nestchat.server.entity.Conversation;
import com.nestchat.server.entity.Relation;
import com.nestchat.server.entity.RelationApplication;
import com.nestchat.server.entity.User;
import com.nestchat.server.mapper.ConversationMapper;
import com.nestchat.server.mapper.RelationApplicationMapper;
import com.nestchat.server.mapper.RelationMapper;
import com.nestchat.server.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class RelationService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RelationMapper relationMapper;
    private final RelationApplicationMapper applicationMapper;
    private final UserMapper userMapper;
    private final ConversationMapper conversationMapper;

    public RelationService(RelationMapper relationMapper, RelationApplicationMapper applicationMapper,
                           UserMapper userMapper, ConversationMapper conversationMapper) {
        this.relationMapper = relationMapper;
        this.applicationMapper = applicationMapper;
        this.userMapper = userMapper;
        this.conversationMapper = conversationMapper;
    }

    public RelationStatusResponse getCurrentRelation(String userId) {
        Relation relation = relationMapper.selectBoundByUserId(userId);
        if (relation == null) {
            // 检查是否有 pending 申请
            Long pendingCount = applicationMapper.selectCount(
                    new LambdaQueryWrapper<RelationApplication>()
                            .eq(RelationApplication::getStatus, "pending")
                            .and(w -> w.eq(RelationApplication::getInitiatorUserId, userId)
                                    .or().eq(RelationApplication::getTargetUserId, userId)));
            RelationStatusResponse resp = new RelationStatusResponse();
            resp.setStatus(pendingCount > 0 ? "pending" : "none");
            return resp;
        }
        return buildRelationStatus(relation, userId);
    }

    @Transactional
    public RelationStatusResponse createApplication(String userId, CreateBindRequest req) {
        // 查找目标用户
        User target = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getAccount, req.getTargetPhone()));
        if (target == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该手机号未注册");
        }
        if (target.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能绑定自己");
        }

        // 检查是否已有绑定关系
        Relation existingRelation = relationMapper.selectBoundByUserId(userId);
        if (existingRelation != null) {
            throw new BusinessException(ResultCode.BINDING_EXISTS);
        }
        Relation targetRelation = relationMapper.selectBoundByUserId(target.getUserId());
        if (targetRelation != null) {
            throw new BusinessException(ResultCode.BINDING_EXISTS, "对方已有绑定关系");
        }

        // 检查是否有待处理的申请
        Long pendingCount = applicationMapper.selectCount(
                new LambdaQueryWrapper<RelationApplication>()
                        .eq(RelationApplication::getStatus, "pending")
                        .and(w -> w
                                .and(w2 -> w2.eq(RelationApplication::getInitiatorUserId, userId)
                                        .eq(RelationApplication::getTargetUserId, target.getUserId()))
                                .or(w2 -> w2.eq(RelationApplication::getInitiatorUserId, target.getUserId())
                                        .eq(RelationApplication::getTargetUserId, userId))));
        if (pendingCount > 0) {
            throw new BusinessException(ResultCode.APPLICATION_EXISTS);
        }

        // 创建申请
        RelationApplication app = new RelationApplication();
        app.setApplicationId(IdGenerator.generate("ra"));
        app.setInitiatorUserId(userId);
        app.setTargetUserId(target.getUserId());
        app.setStatus("pending");
        app.setCreatedAt(LocalDateTime.now());
        applicationMapper.insert(app);

        return getCurrentRelation(userId);
    }

    public List<RelationApplicationResponse> getApplications(String userId) {
        List<RelationApplication> apps = applicationMapper.selectList(
                new LambdaQueryWrapper<RelationApplication>()
                        .eq(RelationApplication::getTargetUserId, userId)
                        .eq(RelationApplication::getStatus, "pending")
                        .orderByDesc(RelationApplication::getCreatedAt));

        List<RelationApplicationResponse> result = new ArrayList<>();
        for (RelationApplication app : apps) {
            RelationApplicationResponse resp = new RelationApplicationResponse();
            resp.setApplicationId(app.getApplicationId());
            resp.setStatus(app.getStatus());
            resp.setInitiatorUserId(app.getInitiatorUserId());
            resp.setTargetUserId(app.getTargetUserId());
            resp.setCreatedAt(app.getCreatedAt().format(DT_FMT));

            User initiator = userMapper.selectById(app.getInitiatorUserId());
            if (initiator != null) {
                resp.setInitiatorPhone(initiator.getAccount());
            }
            User target = userMapper.selectById(app.getTargetUserId());
            if (target != null) {
                resp.setTargetPhone(target.getAccount());
            }
            result.add(resp);
        }
        return result;
    }

    @Transactional
    public RelationStatusResponse acceptApplication(String userId, String applicationId) {
        RelationApplication app = applicationMapper.selectById(applicationId);
        if (app == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "申请不存在");
        }
        if (!app.getTargetUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (!"pending".equals(app.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "申请已处理");
        }

        // 更新申请状态
        app.setStatus("accepted");
        applicationMapper.updateById(app);

        // 创建关系
        LocalDateTime now = LocalDateTime.now();
        Relation relation = new Relation();
        relation.setRelationId(IdGenerator.generate("r"));
        relation.setUserIdA(app.getInitiatorUserId());
        relation.setUserIdB(app.getTargetUserId());
        relation.setRemarkA("");
        relation.setRemarkB("");
        relation.setStatus("bound");
        relation.setBoundAt(now);
        relation.setCreatedAt(now);
        relationMapper.insert(relation);

        // 自动创建会话
        Conversation conv = new Conversation();
        conv.setConversationId(IdGenerator.generate("c"));
        conv.setUserIdA(app.getInitiatorUserId());
        conv.setUserIdB(app.getTargetUserId());
        conv.setCreatedAt(now);
        conversationMapper.insert(conv);

        return getCurrentRelation(userId);
    }

    public void rejectApplication(String userId, String applicationId) {
        RelationApplication app = applicationMapper.selectById(applicationId);
        if (app == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "申请不存在");
        }
        if (!app.getTargetUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (!"pending".equals(app.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "申请已处理");
        }
        app.setStatus("rejected");
        applicationMapper.updateById(app);
    }

    public RelationStatusResponse updateRemark(String userId, UpdateRemarkRequest req) {
        Relation relation = relationMapper.selectBoundByUserId(userId);
        if (relation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "当前无绑定关系");
        }

        if (relation.getUserIdA().equals(userId)) {
            relation.setRemarkA(req.getRemark());
        } else {
            relation.setRemarkB(req.getRemark());
        }
        relationMapper.updateById(relation);
        return buildRelationStatus(relation, userId);
    }

    @Transactional
    public void unbind(String userId) {
        Relation relation = relationMapper.selectBoundByUserId(userId);
        if (relation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "当前无绑定关系");
        }
        relation.setStatus("unbound");
        relationMapper.updateById(relation);
    }

    private RelationStatusResponse buildRelationStatus(Relation relation, String userId) {
        boolean isA = relation.getUserIdA().equals(userId);
        String partnerId = isA ? relation.getUserIdB() : relation.getUserIdA();
        String remark = isA ? relation.getRemarkA() : relation.getRemarkB();

        User partner = userMapper.selectById(partnerId);

        RelationStatusResponse resp = new RelationStatusResponse();
        resp.setRelationId(relation.getRelationId());
        resp.setStatus(relation.getStatus());
        resp.setPartnerUserId(partnerId);
        resp.setPartnerRemark(remark);
        if (partner != null) {
            resp.setPartnerPhone(partner.getAccount());
            resp.setPartnerNickname(partner.getNickname());
            resp.setPartnerAvatarUrl(partner.getAvatarUrl());
        }
        if (relation.getBoundAt() != null) {
            resp.setBoundAt(relation.getBoundAt().format(DT_FMT));
            resp.setCompanionDays(ChronoUnit.DAYS.between(relation.getBoundAt().toLocalDate(),
                    java.time.LocalDate.now()));
        }
        return resp;
    }
}
