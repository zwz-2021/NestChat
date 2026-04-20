CREATE DATABASE IF NOT EXISTS `nestchat`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `nestchat`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `message`;
DROP TABLE IF EXISTS `conversation`;
DROP TABLE IF EXISTS `diary_image`;
DROP TABLE IF EXISTS `diary`;
DROP TABLE IF EXISTS `relation_application`;
DROP TABLE IF EXISTS `relation`;
DROP TABLE IF EXISTS `file_record`;
DROP TABLE IF EXISTS `user`;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `user` (
    `user_id`        VARCHAR(32)  NOT NULL COMMENT '用户 ID',
    `account`        VARCHAR(32)  NOT NULL COMMENT '手机号/登录账号',
    `password`       VARCHAR(128) NOT NULL COMMENT 'BCrypt 密码哈希',
    `nickname`       VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '昵称',
    `avatar_url`     VARCHAR(512) NOT NULL DEFAULT '' COMMENT '头像存储路径',
    `mood_code`      VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '当前情绪编码',
    `mood_text`      VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '当前情绪文案',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `last_active_at` DATETIME              DEFAULT NULL COMMENT '最近活跃时间',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_user_account` (`account`),
    KEY `idx_user_last_active` (`last_active_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `relation` (
    `relation_id` VARCHAR(32) NOT NULL COMMENT '关系 ID',
    `user_id_a`   VARCHAR(32) NOT NULL COMMENT '关系一方用户 ID',
    `user_id_b`   VARCHAR(32) NOT NULL COMMENT '关系另一方用户 ID',
    `remark_a`    VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'A 对 B 的备注',
    `remark_b`    VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'B 对 A 的备注',
    `status`      VARCHAR(16) NOT NULL DEFAULT 'bound' COMMENT '关系状态',
    `bound_at`    DATETIME             DEFAULT NULL COMMENT '绑定时间',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`relation_id`),
    UNIQUE KEY `uk_relation_pair` (`user_id_a`, `user_id_b`),
    KEY `idx_relation_user_a_status` (`user_id_a`, `status`),
    KEY `idx_relation_user_b_status` (`user_id_b`, `status`),
    CONSTRAINT `fk_relation_user_a` FOREIGN KEY (`user_id_a`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_relation_user_b` FOREIGN KEY (`user_id_b`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='绑定关系表';

CREATE TABLE `relation_application` (
    `application_id`    VARCHAR(32) NOT NULL COMMENT '申请 ID',
    `initiator_user_id` VARCHAR(32) NOT NULL COMMENT '发起人用户 ID',
    `target_user_id`    VARCHAR(32) NOT NULL COMMENT '目标用户 ID',
    `type`              VARCHAR(16) NOT NULL DEFAULT 'bind' COMMENT '申请类型：bind/unbind',
    `status`            VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '申请状态：pending/accepted/rejected',
    `created_at`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`application_id`),
    KEY `idx_relation_app_target_status_created` (`target_user_id`, `status`, `created_at` DESC),
    KEY `idx_relation_app_initiator_status_created` (`initiator_user_id`, `status`, `created_at` DESC),
    KEY `idx_relation_app_pair_type_status` (`initiator_user_id`, `target_user_id`, `type`, `status`),
    CONSTRAINT `fk_relation_app_initiator` FOREIGN KEY (`initiator_user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_relation_app_target` FOREIGN KEY (`target_user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关系申请表';

CREATE TABLE `diary` (
    `diary_id`    VARCHAR(32) NOT NULL COMMENT '日记 ID',
    `user_id`     VARCHAR(32) NOT NULL COMMENT '作者用户 ID',
    `date`        VARCHAR(16) NOT NULL COMMENT '日记日期，兼容 yyyy.MM.dd / yyyy-MM-dd',
    `mood_code`   VARCHAR(16) NOT NULL DEFAULT '' COMMENT '情绪编码',
    `mood_text`   VARCHAR(32) NOT NULL DEFAULT '' COMMENT '情绪文案',
    `content`     TEXT COMMENT '日记正文',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`diary_id`),
    KEY `idx_diary_user_date_created` (`user_id`, `date` DESC, `created_at` DESC),
    CONSTRAINT `fk_diary_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日记表';

CREATE TABLE `diary_image` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `diary_id`   VARCHAR(32)  NOT NULL COMMENT '日记 ID',
    `image_url`  VARCHAR(512) NOT NULL COMMENT '图片存储路径',
    `sort_order` INT          NOT NULL DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (`id`),
    KEY `idx_diary_image_diary_sort` (`diary_id`, `sort_order`),
    CONSTRAINT `fk_diary_image_diary` FOREIGN KEY (`diary_id`) REFERENCES `diary` (`diary_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日记图片表';

CREATE TABLE `file_record` (
    `file_id`           VARCHAR(32)  NOT NULL COMMENT '文件 ID',
    `user_id`           VARCHAR(32)  NOT NULL COMMENT '上传用户 ID',
    `file_url`          VARCHAR(512) NOT NULL COMMENT '文件存储路径',
    `thumbnail_url`     VARCHAR(512) NOT NULL DEFAULT '' COMMENT '缩略图路径',
    `mime_type`         VARCHAR(64)  NOT NULL DEFAULT '' COMMENT 'MIME 类型',
    `file_size`         BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小，字节',
    `biz_type`          VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '业务类型',
    `duration_seconds`  INT          NOT NULL DEFAULT 0 COMMENT '音频时长，秒',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`file_id`),
    KEY `idx_file_user_biz_created` (`user_id`, `biz_type`, `created_at` DESC),
    CONSTRAINT `fk_file_record_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上传文件记录表';

CREATE TABLE `conversation` (
    `conversation_id` VARCHAR(32) NOT NULL COMMENT '会话 ID',
    `user_id_a`       VARCHAR(32) NOT NULL COMMENT '会话一方用户 ID',
    `user_id_b`       VARCHAR(32) NOT NULL COMMENT '会话另一方用户 ID',
    `created_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`conversation_id`),
    UNIQUE KEY `uk_conversation_pair` (`user_id_a`, `user_id_b`),
    KEY `idx_conversation_user_a` (`user_id_a`),
    KEY `idx_conversation_user_b` (`user_id_b`),
    CONSTRAINT `fk_conversation_user_a` FOREIGN KEY (`user_id_a`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_conversation_user_b` FOREIGN KEY (`user_id_b`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

CREATE TABLE `message` (
    `message_id`        VARCHAR(32)  NOT NULL COMMENT '消息 ID',
    `conversation_id`   VARCHAR(32)  NOT NULL COMMENT '会话 ID',
    `sender_user_id`    VARCHAR(32)  NOT NULL COMMENT '发送者用户 ID',
    `sender_type`       VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '持久化保留字段，接口层会重算为 me/ta',
    `message_type`      VARCHAR(16)  NOT NULL DEFAULT 'text' COMMENT '消息类型：text/image/voice',
    `content`           TEXT COMMENT '文本内容',
    `image_url`         VARCHAR(512) NOT NULL DEFAULT '' COMMENT '图片路径',
    `voice_url`         VARCHAR(512) NOT NULL DEFAULT '' COMMENT '语音路径',
    `duration_seconds`  INT          NOT NULL DEFAULT 0 COMMENT '语音时长，秒',
    `client_message_id` VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '客户端消息 ID',
    `send_status`       VARCHAR(16)  NOT NULL DEFAULT 'sent' COMMENT '发送状态',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`message_id`),
    KEY `idx_message_conversation_message_id` (`conversation_id`, `message_id` DESC),
    KEY `idx_message_sender_created` (`sender_user_id`, `created_at` DESC),
    CONSTRAINT `fk_message_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`conversation_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_message_sender_user` FOREIGN KEY (`sender_user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';
