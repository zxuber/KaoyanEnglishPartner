package com.kaoyan.peipao.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TranslationTaskResponse {

    private String taskId;

    private String title;

    private String sentence;

    private String hint;

    private String standardTranslation;

    private List<String> checkpoints;
}
