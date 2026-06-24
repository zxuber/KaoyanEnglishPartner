package com.kaoyan.peipao.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MistakeReExplainResponse {
    private Long id;
    private String translation;
}
