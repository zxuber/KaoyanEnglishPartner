package com.kaoyan.peipao.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingArticleResponse {

    private String articleId;
    private String source;
    private String title;
    private String passage;
    private String readingSessionId;
    private Integer wordTranslationLimit;
    private Integer wordTranslationUsed;
    private Integer wordTranslationRemaining;
    private Integer sentenceTranslationLimit;
    private Integer sentenceTranslationUsed;
    private Integer sentenceTranslationRemaining;
    private List<QuestionItem> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionItem {
        private String id;
        private String stem;
        private String focus;
        private List<OptionItem> options;
        private String answer;
        private String explanation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionItem {
        private String label;
        private String content;
    }
}
