package com.kaoyan.peipao.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReadingCoachRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "文章ID不能为空")
    private String articleId;

    @NotBlank(message = "题目ID不能为空")
    private String questionId;

    @NotBlank(message = "用户回答不能为空")
    private String userAnswer;

    private Integer turn;
}
