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
import com.nestchat.server.entity.Diary;
import com.nestchat.server.entity.DiaryImage;
import com.nestchat.server.entity.Message;
import com.nestchat.server.entity.Relation;
import com.nestchat.server.entity.RelationApplication;
import com.nestchat.server.entity.User;
import com.nestchat.server.mapper.ConversationMapper;
import com.nestchat.server.mapper.DiaryImageMapper;
import com.nestchat.server.mapper.DiaryMapper;
import com.nestchat.server.mapper.MessageMapper;
import com.nestchat.server.mapper.RelationApplicationMapper;
import com.nestchat.server.mapper.RelationMapper;
import com.nestchat.server.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class RelationService {

    private static final Logger log = LoggerFactory.getLogger(RelationService.class);
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_BOUND = "bound";
    private static final String STATUS_NONE = "none";
    private static final String STATUS_ACCEPTED = "accepted";
    private static final String STATUS_REJECTED = "rejected";
    private static final String STATUS_UNBOUND = "unbound";
    private static final String TYPE_BIND = "bind";
    private static final String TYPE_UNBIND = "unbind";
    private static final String ROLE_INITIATOR = "initiator";
    private static final String ROLE_TARGET = "target";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RelationMapper relationMapper;
    private final RelationApplicationMapper applicationMapper;
    private final UserMapper userMapper;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final DiaryMapper diaryMapper;
    private final DiaryImageMapper diaryImageMapper;

    public RelationService(RelationMapper relationMapper, RelationApplicationMapper applicationMapper,
                           UserMapper userMapper, ConversationMapper conversationMapper,
                           MessageMapper messageMapper, DiaryMapper diaryMapper,
                           DiaryImageMapper diaryImageMapper) {
        this.relationMapper = relationMapper;
        this.applicationMapper = applicationMapper;
        this.userMapper = userMapper;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.diaryMapper = diaryMapper;
        this.diaryImageMapper = diaryImageMapper;
    }

    public RelationStatusResponse getCurrentRelation(String userId) {
        Relation relation = relationMapper.selectBoundByUserId(userId);
        if (relation == null) {
            RelationApplication pendingBind = findPendingApplicationForUser(userId, TYPE_BIND);
            RelationStatusResponse resp = new RelationStatusResponse();
            resp.setStatus(pendingBind != null ? STATUS_PENDING : STATUS_NONE);
            applyPendingApplicationInfo(resp, pendingBind, userId);
            return resp;
        }

        RelationStatusResponse resp = buildRelationStatus(relation, userId);
        RelationApplication pendingUnbind = findPendingApplicationBetweenUsers(
                relation.getUserIdA(), relation.getUserIdB(), TYPE_UNBIND);
        applyPendingApplicationInfo(resp, pendingUnbind, userId);
        return resp;
    }

    @Transactional
    public RelationStatusResponse createApplication(String userId, CreateBindRequest req) {
        User target = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getAccount, req.getTargetPhone()));
        if (target == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该手机号未注册");
        }
        if (target.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能绑定自己");
        }

        Relation existingRelation = relationMapper.selectBoundByUserId(userId);
        if (existingRelation != null) {
            throw new BusinessException(ResultCode.BINDING_EXISTS);
        }
        Relation targetRelation = relationMapper.selectBoundByUserId(target.getUserId());
        if (targetRelation != null) {
            throw new BusinessException(ResultCode.BINDING_EXISTS, "对方已有绑定关系");
        }

        RelationApplication pendingBind = findPendingApplicationBetweenUsers(userId, target.getUserId(), TYPE_BIND);
        if (pendingBind != null) {
            throw new BusinessException(ResultCode.APPLICATION_EXISTS);
        }

        RelationApplication app = new RelationApplication();
        app.setApplicationId(IdGenerator.generate("ra"));
        app.setInitiatorUserId(userId);
        app.setTargetUserId(target.getUserId());
        app.setType(TYPE_BIND);
        app.setStatus(STATUS_PENDING);
        app.setCreatedAt(LocalDateTime.now());
        applicationMapper.insert(app);

        return getCurrentRelation(userId);
    }

    @Transactional
    public RelationStatusResponse requestUnbind(String userId) {
        Relation relation = relationMapper.selectBoundByUserId(userId);
        if (relation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "当前没有可解绑的关系");
        }

        String partnerId = relation.getUserIdA().equals(userId) ? relation.getUserIdB() : relation.getUserIdA();
        RelationApplication pendingUnbind = findPendingApplicationBetweenUsers(userId, partnerId, TYPE_UNBIND);
        if (pendingUnbind != null) {
            throw new BusinessException(ResultCode.APPLICATION_EXISTS, "解绑申请已存在，请等待对方处理");
        }

        RelationApplication app = new RelationApplication();
        app.setApplicationId(IdGenerator.generate("ru"));
        app.setInitiatorUserId(userId);
        app.setTargetUserId(partnerId);
        app.setType(TYPE_UNBIND);
        app.setStatus(STATUS_PENDING);
        app.setCreatedAt(LocalDateTime.now());
        applicationMapper.insert(app);

        return getCurrentRelation(userId);
    }

    public List<RelationApplicationResponse> getApplications(String userId) {
        List<RelationApplication> apps = applicationMapper.selectList(
                new LambdaQueryWrapper<RelationApplication>()
                        .eq(RelationApplication::getTargetUserId, userId)
                        .eq(RelationApplication::getStatus, STATUS_PENDING)
                        .orderByDesc(RelationApplication::getCreatedAt));

        List<RelationApplicationResponse> result = new ArrayList<>();
        for (RelationApplication app : apps) {
            RelationApplicationResponse resp = new RelationApplicationResponse();
            resp.setApplicationId(app.getApplicationId());
            resp.setType(app.getType());
            resp.setStatus(app.getStatus());
            resp.setInitiatorUserId(app.getInitiatorUserId());
            resp.setTargetUserId(app.getTargetUserId());
            resp.setCreatedAt(formatDateTime(app.getCreatedAt()));

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
        RelationApplication app = getPendingTargetApplication(userId, applicationId);
        app.setStatus(STATUS_ACCEPTED);
        applicationMapper.updateById(app);

        if (TYPE_UNBIND.equals(app.getType())) {
            // 删除双方的所有聊天记录和日记
            Relation relation = getBoundRelationOrThrow(app.getInitiatorUserId(), app.getTargetUserId());
            deleteRelationData(relation);

            // 删除关系记录
            relationMapper.deleteById(relation.getRelationId());
            return getCurrentRelation(userId);
        }

        LocalDateTime now = LocalDateTime.now();
        Relation relation = new Relation();
        relation.setRelationId(IdGenerator.generate("r"));
        relation.setUserIdA(app.getInitiatorUserId());
        relation.setUserIdB(app.getTargetUserId());
        relation.setRemarkA("");
        relation.setRemarkB("");
        relation.setStatus(STATUS_BOUND);
        relation.setBoundAt(now);
        relation.setCreatedAt(now);
        relationMapper.insert(relation);

        Conversation conv = new Conversation();
        conv.setConversationId(IdGenerator.generate("c"));
        conv.setUserIdA(app.getInitiatorUserId());
        conv.setUserIdB(app.getTargetUserId());
        conv.setCreatedAt(now);
        conversationMapper.insert(conv);

        return getCurrentRelation(userId);
    }

    /**
     * 删除关系相关的所有数据（聊天记录、日记等）
     */
    private void deleteRelationData(Relation relation) {
        String userIdA = relation.getUserIdA();
        String userIdB = relation.getUserIdB();

        log.info("开始删除关系数据，relationId={}, userIdA={}, userIdB={}",
                relation.getRelationId(), userIdA, userIdB);

        // 1. 查找并删除会话
        Conversation conversation = conversationMapper.selectByUserIds(userIdA, userIdB);
        if (conversation != null) {
            String conversationId = conversation.getConversationId();

            // 2. 删除该会话的所有消息
            int deletedMessages = messageMapper.delete(
                    new LambdaQueryWrapper<Message>()
                            .eq(Message::getConversationId, conversationId)
            );
            log.info("删除消息记录，conversationId={}, count={}", conversationId, deletedMessages);

            // 3. 删除会话记录
            conversationMapper.deleteById(conversationId);
            log.info("删除会话记录，conversationId={}", conversationId);
        }

        // 4. 删除用户A的所有日记和日记图片
        deleteUserData(userIdA);

        // 5. 删除用户B的所有日记和日记图片
        deleteUserData(userIdB);

        log.info("关系数据删除完成，relationId={}", relation.getRelationId());
    }

    /**
     * 删除单个用户的所有日记和相关图片
     */
    private void deleteUserData(String userId) {
        // 查询该用户的所有日记
        List<Diary> diaries = diaryMapper.selectList(
                new LambdaQueryWrapper<Diary>()
                        .eq(Diary::getUserId, userId)
        );

        if (!diaries.isEmpty()) {
            // 删除所有日记的图片记录
            for (Diary diary : diaries) {
                int deletedImages = diaryImageMapper.delete(
                        new LambdaQueryWrapper<DiaryImage>()
                                .eq(DiaryImage::getDiaryId, diary.getDiaryId())
                );
                log.debug("删除日记图片，diaryId={}, count={}", diary.getDiaryId(), deletedImages);
            }

            // 删除日记记录
            int deletedDiaries = diaryMapper.delete(
                    new LambdaQueryWrapper<Diary>()
                            .eq(Diary::getUserId, userId)
            );
            log.info("删除用户日记，userId={}, count={}", userId, deletedDiaries);
        }
    }

    public void rejectApplication(String userId, String applicationId) {
        RelationApplication app = getPendingTargetApplication(userId, applicationId);
        app.setStatus(STATUS_REJECTED);
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
        return getCurrentRelation(userId);
    }

    private RelationApplication getPendingTargetApplication(String userId, String applicationId) {
        RelationApplication app = applicationMapper.selectById(applicationId);
        if (app == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "申请不存在");
        }
        if (!userId.equals(app.getTargetUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (!STATUS_PENDING.equals(app.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "申请已处理");
        }
        return app;
    }

    private RelationApplication findPendingApplicationForUser(String userId, String type) {
        return applicationMapper.selectOne(
                new LambdaQueryWrapper<RelationApplication>()
                        .eq(RelationApplication::getStatus, STATUS_PENDING)
                        .eq(RelationApplication::getType, type)
                        .and(w -> w.eq(RelationApplication::getInitiatorUserId, userId)
                                .or().eq(RelationApplication::getTargetUserId, userId))
                        .orderByDesc(RelationApplication::getCreatedAt)
                        .last("LIMIT 1"));
    }

    private RelationApplication findPendingApplicationBetweenUsers(String userIdA, String userIdB, String type) {
        return applicationMapper.selectOne(
                new LambdaQueryWrapper<RelationApplication>()
                        .eq(RelationApplication::getStatus, STATUS_PENDING)
                        .eq(RelationApplication::getType, type)
                        .and(w -> w
                                .and(w2 -> w2.eq(RelationApplication::getInitiatorUserId, userIdA)
                                        .eq(RelationApplication::getTargetUserId, userIdB))
                                .or(w2 -> w2.eq(RelationApplication::getInitiatorUserId, userIdB)
                                        .eq(RelationApplication::getTargetUserId, userIdA)))
                        .orderByDesc(RelationApplication::getCreatedAt)
                        .last("LIMIT 1"));
    }

    private Relation getBoundRelationOrThrow(String userIdA, String userIdB) {
        Relation relation = relationMapper.selectBoundByUserId(userIdA);
        if (relation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "当前无绑定关系");
        }
        boolean matched = (userIdA.equals(relation.getUserIdA()) && userIdB.equals(relation.getUserIdB()))
                || (userIdA.equals(relation.getUserIdB()) && userIdB.equals(relation.getUserIdA()));
        if (!matched) {
            throw new BusinessException(ResultCode.NOT_FOUND, "当前无绑定关系");
        }
        return relation;
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
            resp.setBoundAt(formatDateTime(relation.getBoundAt()));
            resp.setCompanionDays(ChronoUnit.DAYS.between(relation.getBoundAt().toLocalDate(), LocalDate.now()));
        }
        return resp;
    }

    private void applyPendingApplicationInfo(RelationStatusResponse resp, RelationApplication app, String userId) {
        if (resp == null || app == null) {
            return;
        }
        resp.setPendingApplicationId(app.getApplicationId());
        resp.setPendingApplicationType(app.getType());
        resp.setPendingApplicationRole(userId.equals(app.getInitiatorUserId()) ? ROLE_INITIATOR : ROLE_TARGET);
        resp.setPendingApplicationCreatedAt(formatDateTime(app.getCreatedAt()));
    }

    private String formatDateTime(LocalDateTime time) {
        return time == null ? null : time.format(DT_FMT);
    }
}
