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
public class DashboardResponse {

    private String greeting;
    private boolean onboardingDone;
    private ContinueTraining continueTraining;
    private List<TaskCard> todayTasks;
    private List<QuickEntry> quickEntries;
    private List<ReviewItem> reviewItems;
    private Stats stats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContinueTraining {
        private String module;
        private String title;
        private String subtitle;
        private String page;
        private Integer progressCurrent;
        private Integer progressTotal;
        private String accent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskCard {
        private String title;
        private String subtitle;
        private String reason;
        private String page;
        private String badge;
        private String accent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickEntry {
        private String title;
        private String subtitle;
        private String page;
        private String accent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewItem {
        private String label;
        private String value;
        private String hint;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stats {
        private Integer masteredWords;
        private Integer totalCheckins;
        private Integer targetScore;
        private String currentPhase;
    }
}
