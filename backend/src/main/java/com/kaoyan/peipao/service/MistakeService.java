package com.kaoyan.peipao.service;

import com.kaoyan.peipao.dto.request.AddMistakeItemRequest;
import com.kaoyan.peipao.dto.response.MistakeItemResponse;
import com.kaoyan.peipao.entity.MistakeItem;
import com.kaoyan.peipao.mapper.MistakeItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MistakeService {

    private final MistakeItemMapper mistakeItemMapper;
    private final LLMService llmService;

    public MistakeItemResponse addItem(AddMistakeItemRequest request) {
        MistakeItem existing = mistakeItemMapper.selectActiveBySource(
                request.getUserId(),
                normalizeType(request.getType()),
                request.getSourceText().trim()
        );
        if (existing != null) {
            return toResponse(existing);
        }

        MistakeItem item = new MistakeItem();
        item.setUserId(request.getUserId());
        item.setType(normalizeType(request.getType()));
        item.setSourceText(request.getSourceText().trim());
        item.setTranslation(resolveTranslation(request.getTranslation(), request.getSourceText(), request.getType()));
        item.setSourceModule(request.getSourceModule() == null || request.getSourceModule().isBlank() ? "阅读" : request.getSourceModule());
        item.setArticleKey(request.getArticleId());
        item.setStatus("active");
        mistakeItemMapper.insert(item);
        return toResponse(item);
    }

    public List<MistakeItemResponse> listByType(Long userId, String type) {
        return mistakeItemMapper.selectActiveByUserAndType(userId, normalizeType(type)).stream()
                .map(this::toResponse)
                .toList();
    }

    private String resolveTranslation(String translation, String sourceText, String type) {
        if (translation != null && !translation.isBlank()) {
            return translation.trim();
        }
        return llmService.translateSelection(sourceText, normalizeType(type));
    }

    private String normalizeType(String type) {
        return "sentence".equalsIgnoreCase(type) ? "sentence" : "word";
    }

    private MistakeItemResponse toResponse(MistakeItem item) {
        return MistakeItemResponse.builder()
                .id(item.getId())
                .type(item.getType())
                .sourceText(item.getSourceText())
                .translation(item.getTranslation())
                .sourceModule(item.getSourceModule())
                .articleId(item.getArticleKey())
                .status(item.getStatus())
                .createdAt(item.getCreatedAt() == null ? "" : item.getCreatedAt().toString())
                .build();
    }
}
