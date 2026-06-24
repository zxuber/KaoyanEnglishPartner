package com.kaoyan.peipao.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddMistakeItemRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "类型不能为空")
    private String type;

    @NotBlank(message = "原文不能为空")
    private String sourceText;

    private String translation;

    private String sourceModule;

    private String articleId;
}
