package com.kaoyan.peipao.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateRecentTrainingRequest {

    @NotBlank(message = "训练模块不能为空")
    private String module;

    @NotBlank(message = "训练标题不能为空")
    private String title;

    private String subtitle;

    @NotBlank(message = "训练页面不能为空")
    private String page;

    private Integer progressCurrent;

    private Integer progressTotal;

    private String accent;
}
