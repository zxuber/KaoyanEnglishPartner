package com.kaoyan.peipao.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WordVO {
    private Long id;
    private String word;
    private String meaning;
    private Integer unit;
    private Integer page;
}
