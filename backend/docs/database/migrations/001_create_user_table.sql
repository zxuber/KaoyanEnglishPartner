-- ============================================
-- 001: 用户表 (M1 入门问卷)
-- 创建日期: 2026-06-20
-- ============================================

CREATE TABLE IF NOT EXISTS `user` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `openid`        VARCHAR(64)     DEFAULT NULL             COMMENT '微信 openid (M5 用户认证后填入)',
    `session_id`    VARCHAR(64)     DEFAULT NULL             COMMENT '匿名会话标识 (M1 临时方案，M5 完成后废弃)',
    `nickname`      VARCHAR(64)     DEFAULT NULL             COMMENT '微信昵称',
    `avatar_url`    VARCHAR(512)    DEFAULT NULL             COMMENT '微信头像 URL',

    -- Q1-Q7 入门问卷答案
    `exam_type`         VARCHAR(16)     DEFAULT NULL  COMMENT 'Q1: 英语一 / 英语二',
    `target_score`      INT             DEFAULT NULL  COMMENT 'Q2: 目标分数 40-80',
    `remaining_days`    INT             DEFAULT NULL  COMMENT 'Q3: 距考试剩余天数',
    `daily_hours`       VARCHAR(16)     DEFAULT NULL  COMMENT 'Q4: 1h / 1.5h / 2h / 2.5h / 3h+',
    `weekend_available` VARCHAR(16)     DEFAULT NULL  COMMENT 'Q5: 有时间 / 没时间 / 不确定',
    `english_level`     VARCHAR(32)     DEFAULT NULL  COMMENT 'Q6: 四级未过 / 四级低分飘过 / 四级高分 / 六级已过 / 六级高分',
    `materials`         JSON            DEFAULT NULL  COMMENT 'Q7: 已有资料列表 ["红宝书","刘晓燕系列"]',

    -- LLM 生成的专属方案
    `plan_json`         JSON            DEFAULT NULL  COMMENT 'LLM 生成的专属学习方案 (分数拆解+阶段节奏+每日任务+里程碑)',
    `plan_generated_at` DATETIME        DEFAULT NULL  COMMENT '方案生成时间',

    -- 学习进度
    `current_phase`     VARCHAR(16)     DEFAULT 'phase1'  COMMENT '当前阶段: phase1/phase2/phase3',
    `phase_start_day`   INT             DEFAULT 1         COMMENT '当前阶段起始天数 (从第几天进入的)',
    `total_checkins`    INT             DEFAULT 0         COMMENT '累计打卡天数',
    `last_checkin_date` DATE            DEFAULT NULL      COMMENT '最后打卡日期',

    `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
