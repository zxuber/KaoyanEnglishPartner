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
public class PlanResponse {
    private String profile;
    private List<String> diagnosisSummary;
    private List<ScoreItem> scoreBreakdown;
    private List<PhaseItem> phases;
    private List<WeekFocusItem> weeklyFocus;
    private String weekdayTemplate;
    private String weekendTemplate;
    private String riskTip;
    private String catchUpAdvice;
    private List<MilestoneItem> milestones;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreItem {
        private String type;
        private int fullScore;
        private int targetScore;
        private int difficulty;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhaseItem {
        private String name;
        private String dayRange;
        private String focus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MilestoneItem {
        private int day;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeekFocusItem {
        private String label;
        private String tasks;
    }
}
