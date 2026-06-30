package com.kaoyan.peipao.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PracticeArchiveSummaryResponse {

    private List<ModuleStat> modules;

    @Data
    @Builder
    public static class ModuleStat {
        private String module;
        private String moduleName;
        private Integer completedCount;
        private Integer importantCount;
        private String title;
        private String subtitle;
    }
}
