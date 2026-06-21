-- ============================================
-- 002: 单词表 (M3 单词闪卡)
-- 数据源: GitHub 3056810551/2027-kaoyan-english-redbook-json
-- 创建日期: 2026-06-20
-- ============================================

CREATE TABLE IF NOT EXISTS `word` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `word`          VARCHAR(128) NOT NULL                 COMMENT '英文单词',
    `meaning`       TEXT         NOT NULL                 COMMENT '中文释义（含词性标注）',
    `page`          INT          DEFAULT 0                COMMENT '红宝书页码',
    `word_index`    INT          DEFAULT 0                COMMENT '页内序号',
    `unit`          INT          DEFAULT 0                COMMENT '所属 Unit（1-26 必考词，27+ 基础词）',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_word` (`word`),
    KEY `idx_unit` (`unit`),
    KEY `idx_page` (`page`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单词表';


-- ============================================
-- 002b: 单词学习进度表
-- ============================================

CREATE TABLE IF NOT EXISTS `word_progress` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `user_id`           BIGINT       NOT NULL                COMMENT '用户 ID',
    `word_id`           BIGINT       NOT NULL                COMMENT '单词 ID',
    `status`            VARCHAR(16)  NOT NULL DEFAULT 'new'  COMMENT '掌握状态: new/learning/mastered',
    `mistake_count`     INT          NOT NULL DEFAULT 0      COMMENT '累计标记次数',
    `correct_streak`    INT          NOT NULL DEFAULT 0      COMMENT '连续正确次数（3 次变 mastered）',
    `next_review_date`  DATE         DEFAULT NULL            COMMENT '下次复习日期（艾宾浩斯）',
    `review_interval`   INT          DEFAULT 0               COMMENT '当前复习间隔（天）',
    `last_reviewed_at`  DATETIME     DEFAULT NULL            COMMENT '最后复习时间',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_word` (`user_id`, `word_id`),
    KEY `idx_next_review` (`user_id`, `next_review_date`),
    KEY `idx_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单词学习进度表';
