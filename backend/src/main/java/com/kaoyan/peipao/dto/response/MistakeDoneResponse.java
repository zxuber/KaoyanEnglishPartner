package com.kaoyan.peipao.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MistakeDoneResponse {
    private String id;
    private String sourceType;
    private String category;
    private String categoryLabel;
    private String sourceText;
    private String translation;
    private String sourceModule;
    private String sourceHint;
    private String note;
    private String status;
    private String doneAt;
}
