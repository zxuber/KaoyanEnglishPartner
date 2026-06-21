package com.kaoyan.peipao.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

/**
 * M1 入门问卷 - 7 题答案
 */
@Data
public class OnboardingRequest {

    @NotBlank(message = "Q1: 请选择英语一或英语二")
    private String examType;

    @NotNull(message = "Q2: 请输入目标分数")
    @Min(value = 40, message = "目标分数最低 40 分")
    @Max(value = 80, message = "目标分数最高 80 分")
    private Integer targetScore;

    @NotNull(message = "Q3: 请输入剩余天数")
    @Min(value = 1, message = "剩余天数至少 1 天")
    @Max(value = 730, message = "剩余天数不超过 730 天")
    private Integer remainingDays;

    @NotBlank(message = "Q4: 请选择每日学习时长")
    private String dailyHours;

    @NotBlank(message = "Q5: 请选择周末是否有时间")
    private String weekendAvailable;

    @NotBlank(message = "Q6: 请选择当前英语水平")
    private String englishLevel;

    private List<String> materials;
}
