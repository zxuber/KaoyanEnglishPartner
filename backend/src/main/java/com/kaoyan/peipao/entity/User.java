package com.kaoyan.peipao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户表 - 对应 M1 学习画像 + LLM 专属方案
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 微信 openid（M5 用户认证后填入，当前可为空） */
    private String openid;

    /** 匿名会话标识（M1 临时方案，M5 完成后废弃） */
    private String sessionId;

    /** 微信昵称 */
    private String nickname;

    /** 微信头像 URL */
    private String avatarUrl;

    // ========== 学习画像 ==========

    private String examType;

    private Integer targetScore;

    private Integer remainingDays;

    /** 最近一次真题/模拟分数区间 */
    private String currentScoreBand;

    /** 当前基础水平 */
    private String englishLevel;

    /** 薄弱模块 JSON */
    private String weakModules;

    /** 工作日学习时长 */
    private String weekdayHours;

    /** 周末学习时长 */
    private String weekendHours;

    /** 最稳定学习时段 */
    private String studyTimeSlot;

    /** 计划风格 */
    private String planStyle;

    /** 当前最大卡点 */
    private String biggestObstacle;

    /** 已有资料列表 JSON */
    private String materials;

    // ========== LLM 生成方案 ==========

    /** LLM 生成的专属学习方案 JSON 字符串 */
    private String planJson;

    /** 方案生成时间 */
    private LocalDateTime planGeneratedAt;

    // ========== 学习进度 ==========

    /** 当前阶段: phase1 / phase2 / phase3 */
    private String currentPhase;

    /** 当前阶段起始天数 */
    private Integer phaseStartDay;

    /** 累计打卡天数 */
    private Integer totalCheckins;

    /** 最后打卡日期 */
    private LocalDate lastCheckinDate;

    // ========== 审计字段 ==========

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
