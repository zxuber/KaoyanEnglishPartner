package com.kaoyan.peipao.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PracticeArchiveItemResponse {

    private Long id;

    private String module;

    private String moduleName;

    private String title;

    private String summary;

    private String selectedOption;

    private String userAnswer;

    private String coachReply;

    private Boolean completed;

    private Boolean important;

    private String trainedAt;
}
