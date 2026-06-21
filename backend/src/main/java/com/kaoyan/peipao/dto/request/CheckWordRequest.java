package com.kaoyan.peipao.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckWordRequest {
    @NotNull private Long userId;
    @NotNull private Long wordId;
    private String userAnswer;
    private Boolean unknown;
}
