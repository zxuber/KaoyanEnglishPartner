package com.kaoyan.peipao.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MistakeAssetResponse {
    private Long id;
    private String category;
    private String subcategory;
    private String sourceText;
    private String translation;
    private String sourceHint;
    private String note;
    private String exampleEn;
    private String exampleZh;
    private String sourceModule;
    private String status;
}
