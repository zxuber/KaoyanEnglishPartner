package com.kaoyan.peipao.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PracticeCoachRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    private String practiceType;

    private List<String> selectedOptionIds;

    private String selectedOption;

    private String userAnswer;

    private String mainStructure;

    private String modifiers;

    private String translation;

    private Integer step;

    private Integer turn;
}
