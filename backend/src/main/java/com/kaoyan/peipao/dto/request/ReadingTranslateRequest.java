package com.kaoyan.peipao.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReadingTranslateRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "文章ID不能为空")
    private String articleId;

    @NotBlank(message = "内容类型不能为空")
    private String contentType;

    @NotBlank(message = "选中文本不能为空")
    private String sourceText;
}
