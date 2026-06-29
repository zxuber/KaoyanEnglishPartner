package com.kaoyan.peipao.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClozeTaskResponse {

    private String taskId;

    private String title;

    private String passage;

    private String stem;

    private List<OptionItem> options;

    @Data
    @Builder
    public static class OptionItem {
        private String label;
        private String content;
    }
}
