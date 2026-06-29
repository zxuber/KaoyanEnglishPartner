package com.kaoyan.peipao.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WritingTaskResponse {

    private String taskId;

    private String writingType;

    private String title;

    private String prompt;

    private String goal;

    private List<ThinkingQuestion> thinkingQuestions;

    @Data
    @Builder
    public static class ThinkingQuestion {
        private String id;
        private String title;
        private List<OptionItem> options;
    }

    @Data
    @Builder
    public static class OptionItem {
        private String id;
        private String label;
        private String content;
    }
}
