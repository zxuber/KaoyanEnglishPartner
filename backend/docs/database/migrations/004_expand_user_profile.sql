-- ============================================
-- 004: 扩展用户学习画像字段
-- 创建日期: 2026-06-22
-- ============================================

ALTER TABLE `user`
    ADD COLUMN `current_score_band` VARCHAR(32) DEFAULT NULL COMMENT '最近一次真题/模拟分数区间' AFTER `remaining_days`,
    ADD COLUMN `weak_modules` JSON DEFAULT NULL COMMENT '薄弱模块列表' AFTER `english_level`,
    ADD COLUMN `weekday_hours` VARCHAR(16) DEFAULT NULL COMMENT '工作日学习时长' AFTER `weak_modules`,
    ADD COLUMN `weekend_hours` VARCHAR(16) DEFAULT NULL COMMENT '周末学习时长' AFTER `weekday_hours`,
    ADD COLUMN `study_time_slot` VARCHAR(32) DEFAULT NULL COMMENT '最稳定学习时段' AFTER `weekend_hours`,
    ADD COLUMN `plan_style` VARCHAR(32) DEFAULT NULL COMMENT '偏好计划风格' AFTER `study_time_slot`,
    ADD COLUMN `biggest_obstacle` VARCHAR(64) DEFAULT NULL COMMENT '当前最大卡点' AFTER `plan_style`;
