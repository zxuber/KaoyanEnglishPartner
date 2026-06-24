package com.kaoyan.peipao.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReadingTranslateResponse {
    private String translatedText;
    private String contentType;
    private Integer limit;
    private Integer usedCount;
    private Integer remainingCount;
}
