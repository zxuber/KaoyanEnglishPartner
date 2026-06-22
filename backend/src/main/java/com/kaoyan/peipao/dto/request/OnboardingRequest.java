package com.kaoyan.peipao.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

/**
 * M1 学习画像采集
 */
@Data
public class OnboardingRequest {

    private Long userId;

    @NotBlank(message = "请选择英语一或英语二")
    private String examType;

    @NotNull(message = "请输入目标分数")
    @Min(value = 40, message = "目标分数最低 40 分")
    @Max(value = 80, message = "目标分数最高 80 分")
    private Integer targetScore;

    @NotNull(message = "请输入剩余天数")
    @Min(value = 1, message = "剩余天数至少 1 天")
    @Max(value = 730, message = "剩余天数不超过 730 天")
    private Integer remainingDays;

    @NotBlank(message = "请选择最近一次真题或模拟分数区间")
    private String currentScoreBand;

    @NotBlank(message = "请选择当前英语基础")
    private String englishLevel;

    @NotEmpty(message = "请至少选择一个薄弱模块")
    private List<String> weakModules;

    @NotBlank(message = "请选择工作日学习时长")
    private String weekdayHours;

    @NotBlank(message = "请选择周末学习时长")
    private String weekendHours;

    @NotBlank(message = "请选择最稳定的学习时段")
    private String studyTimeSlot;

    @NotBlank(message = "请选择更适合你的计划风格")
    private String planStyle;

    @NotBlank(message = "请选择当前最大的卡点")
    private String biggestObstacle;

    private List<String> materials;
}
