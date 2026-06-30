package com.kaoyan.peipao.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PracticeCoachResponse {

    private String coachReply;

    private String nextQuestion;

    private Boolean revealAnswer;

    private String answer;

    private String explanation;

    private WritingFrame writingFrame;

    private List<AssetCandidate> assetCandidates;

    @Data
    @Builder
    public static class AssetCandidate {
        private String type;
        private String sourceText;
        private String translation;
        private String sourceModule;
    }

    @Data
    @Builder
    public static class WritingFrame {
        private String title;
        private String instruction;
        private List<FrameLine> lines;
    }

    @Data
    @Builder
    public static class FrameLine {
        private String id;
        private String beforeText;
        private String afterText;
        private String label;
        private String placeholder;
        private Integer maxLength;
    }
}
