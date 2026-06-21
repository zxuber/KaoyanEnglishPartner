package com.kaoyan.peipao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户表 - 对应 M1 入门问卷 + LLM 专属方案
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

    // ========== Q1-Q7 入门问卷答案 ==========

    /** Q1: 英语一 / 英语二 */
    private String examType;

    /** Q2: 目标分数 40-80 */
    private Integer targetScore;

    /** Q3: 距考试剩余天数 */
    private Integer remainingDays;

    /** Q4: 1h / 1.5h / 2h / 2.5h / 3h+ */
    private String dailyHours;

    /** Q5: 有时间 / 没时间 / 不确定 */
    private String weekendAvailable;

    /** Q6: 四级未过 / 四级低分飘过 / 四级高分 / 六级已过 / 六级高分 */
    private String englishLevel;

    /** Q7: 已有资料列表 JSON ["红宝书","刘晓燕系列"] */
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
