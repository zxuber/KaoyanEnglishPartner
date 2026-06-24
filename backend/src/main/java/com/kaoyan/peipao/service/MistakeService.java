package com.kaoyan.peipao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kaoyan.peipao.common.BizException;
import com.kaoyan.peipao.common.ErrorCode;
import com.kaoyan.peipao.dto.request.AddMistakeItemRequest;
import com.kaoyan.peipao.dto.response.MistakeItemResponse;
import com.kaoyan.peipao.dto.response.MistakeReExplainResponse;
import com.kaoyan.peipao.entity.MistakeItem;
import com.kaoyan.peipao.mapper.MistakeItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MistakeService {

    private final MistakeItemMapper mistakeItemMapper;
    private final LLMService llmService;
    private final TokenActionGuardService tokenActionGuardService;

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

    public MistakeReExplainResponse reExplain(Long userId, Long id) {
        MistakeItem item = mistakeItemMapper.selectOne(new LambdaQueryWrapper<MistakeItem>()
                .eq(MistakeItem::getId, id)
                .eq(MistakeItem::getUserId, userId)
                .eq(MistakeItem::getStatus, "active")
                .last("LIMIT 1"));
        if (item == null) {
            throw new BizException(ErrorCode.MISTAKE_ITEM_NOT_FOUND);
        }

        tokenActionGuardService.guard(userId, "mistake-re-explain", Duration.ofSeconds(8));

        String translation = llmService.translateSelection(item.getSourceText(), normalizeType(item.getType()));
        if (translation == null || translation.isBlank() || "暂未获取翻译，请稍后重试".equals(translation)) {
            throw new BizException(ErrorCode.LLM_GENERATION_FAILED, "重新解释失败，请稍后再试");
        }

        item.setTranslation(translation.trim());
        mistakeItemMapper.updateById(item);
        return MistakeReExplainResponse.builder()
                .id(item.getId())
                .translation(item.getTranslation())
                .build();
    }

    public void markDone(Long userId, Long id) {
        MistakeItem item = mistakeItemMapper.selectOne(new LambdaQueryWrapper<MistakeItem>()
                .eq(MistakeItem::getId, id)
                .eq(MistakeItem::getUserId, userId)
                .eq(MistakeItem::getStatus, "active")
                .last("LIMIT 1"));
        if (item == null) {
            throw new BizException(ErrorCode.MISTAKE_ITEM_NOT_FOUND);
        }
        item.setStatus("done");
        mistakeItemMapper.updateById(item);
    }

    public List<String> listActiveMistakeWords(Long userId, int limit) {
        return mistakeItemMapper.selectActiveWordMistakes(userId, limit).stream()
                .map(MistakeItem::getSourceText)
                .map(this::normalizeWord)
                .filter(word -> !word.isBlank())
                .distinct()
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

    private String normalizeWord(String sourceText) {
        return sourceText == null
                ? ""
                : sourceText.trim().toLowerCase(Locale.ROOT).replaceAll("^[^a-z]+|[^a-z'-]+$", "");
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
