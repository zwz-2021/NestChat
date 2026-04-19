CREATE DATABASE IF NOT EXISTS nestchat DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nestchat;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `user_id`    VARCHAR(32)  NOT NULL,
    `account`    VARCHAR(32)  NOT NULL COMMENT '手机号',
    `password`   VARCHAR(128) NOT NULL,
    `nickname`   VARCHAR(64)  DEFAULT '',
    `avatar_url` VARCHAR(512) DEFAULT '',
    `mood_code`  VARCHAR(16)  DEFAULT '',
    `mood_text`  VARCHAR(32)  DEFAULT '',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_account` (`account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 关系表
CREATE TABLE IF NOT EXISTS `relation` (
    `relation_id` VARCHAR(32)  NOT NULL,
    `user_id_a`   VARCHAR(32)  NOT NULL,
    `user_id_b`   VARCHAR(32)  NOT NULL,
    `remark_a`    VARCHAR(64)  DEFAULT '' COMMENT 'A对B的备注',
    `remark_b`    VARCHAR(64)  DEFAULT '' COMMENT 'B对A的备注',
    `status`      VARCHAR(16)  NOT NULL DEFAULT 'bound',
    `bound_at`    DATETIME     DEFAULT NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`relation_id`),
    KEY `idx_user_a` (`user_id_a`),
    KEY `idx_user_b` (`user_id_b`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 关系申请表
CREATE TABLE IF NOT EXISTS `relation_application` (
    `application_id`   VARCHAR(32) NOT NULL,
    `initiator_user_id` VARCHAR(32) NOT NULL,
    `target_user_id`   VARCHAR(32) NOT NULL,
    `status`           VARCHAR(16) NOT NULL DEFAULT 'pending',
    `created_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`application_id`),
    KEY `idx_target` (`target_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 日记表
CREATE TABLE IF NOT EXISTS `diary` (
    `diary_id`   VARCHAR(32)  NOT NULL,
    `user_id`    VARCHAR(32)  NOT NULL,
    `date`       VARCHAR(16)  NOT NULL COMMENT '格式: 2026.04.18',
    `mood_code`  VARCHAR(16)  DEFAULT '',
    `mood_text`  VARCHAR(32)  DEFAULT '',
    `content`    TEXT,
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`diary_id`),
    KEY `idx_user_date` (`user_id`, `date` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 日记图片表
CREATE TABLE IF NOT EXISTS `diary_image` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `diary_id`   VARCHAR(32)  NOT NULL,
    `image_url`  VARCHAR(512) NOT NULL,
    `sort_order` INT          DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_diary` (`diary_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 文件记录表
CREATE TABLE IF NOT EXISTS `file_record` (
    `file_id`          VARCHAR(32)  NOT NULL,
    `user_id`          VARCHAR(32)  NOT NULL,
    `file_url`         VARCHAR(512) NOT NULL,
    `thumbnail_url`    VARCHAR(512) DEFAULT '',
    `mime_type`        VARCHAR(64)  DEFAULT '',
    `file_size`        BIGINT       DEFAULT 0,
    `biz_type`         VARCHAR(16)  DEFAULT '',
    `duration_seconds` INT          DEFAULT 0,
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 会话表
CREATE TABLE IF NOT EXISTS `conversation` (
    `conversation_id` VARCHAR(32) NOT NULL,
    `user_id_a`       VARCHAR(32) NOT NULL,
    `user_id_b`       VARCHAR(32) NOT NULL,
    `created_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`conversation_id`),
    KEY `idx_user_a` (`user_id_a`),
    KEY `idx_user_b` (`user_id_b`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 消息表
CREATE TABLE IF NOT EXISTS `message` (
    `message_id`        VARCHAR(32)  NOT NULL,
    `conversation_id`   VARCHAR(32)  NOT NULL,
    `sender_user_id`    VARCHAR(32)  NOT NULL,
    `sender_type`       VARCHAR(16)  DEFAULT '',
    `message_type`      VARCHAR(16)  NOT NULL DEFAULT 'text',
    `content`           TEXT,
    `image_url`         VARCHAR(512) DEFAULT '',
    `voice_url`         VARCHAR(512) DEFAULT '',
    `duration_seconds`  INT          DEFAULT 0,
    `client_message_id` VARCHAR(64)  DEFAULT '',
    `send_status`       VARCHAR(16)  NOT NULL DEFAULT 'sent',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`message_id`),
    KEY `idx_conversation_time` (`conversation_id`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
