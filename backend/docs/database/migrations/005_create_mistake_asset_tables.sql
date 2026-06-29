CREATE TABLE IF NOT EXISTS `mistake_asset_library` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `category`      VARCHAR(32)  NOT NULL COMMENT 'writing / phrase / confusion',
    `subcategory`   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '更细的二级分类',
    `source_text`   VARCHAR(255) NOT NULL COMMENT '英文原文或易混组合',
    `translation`   VARCHAR(500) NOT NULL COMMENT '中文释义或辨析',
    `source_hint`   VARCHAR(255) NOT NULL DEFAULT '' COMMENT '来源提示或适用场景',
    `note`          VARCHAR(500) NOT NULL DEFAULT '' COMMENT '补充说明',
    `example_en`    VARCHAR(500) NOT NULL DEFAULT '' COMMENT '英文例句',
    `example_zh`    VARCHAR(500) NOT NULL DEFAULT '' COMMENT '中文例句',
    `source_module` VARCHAR(64)  NOT NULL DEFAULT '系统资产库' COMMENT '默认来源模块',
    `sort_order`    INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `active`        TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mistake_asset_category_text` (`category`, `source_text`),
    KEY `idx_mistake_asset_category_active` (`category`, `active`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='误解本全局资产库';

CREATE TABLE IF NOT EXISTS `mistake_asset_progress` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT      NOT NULL COMMENT '用户ID',
    `asset_id`   BIGINT      NOT NULL COMMENT '资产ID',
    `status`     VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active / done',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mistake_asset_progress_user_asset` (`user_id`, `asset_id`),
    KEY `idx_mistake_asset_progress_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='误解本资产用户进度';
