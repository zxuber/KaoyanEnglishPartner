package com.kaoyan.peipao.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MistakeItemResponse {
    private Long id;
    private String type;
    private String sourceText;
    private String translation;
    private String sourceModule;
    private String articleId;
    private String status;
    private String createdAt;
}
