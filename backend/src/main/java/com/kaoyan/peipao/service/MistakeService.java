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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class MistakeService {

    private final MistakeItemMapper mistakeItemMapper;
    private final LLMService llmService;
    private final TokenActionGuardService tokenActionGuardService;

    public MistakeItemResponse addItem(AddMistakeItemRequest request) {
        String normalizedType = normalizeType(request.getType());
        String normalizedSourceText = request.getSourceText().trim();
        log.info("[误解本] add start userId={}, type={}, sourceModule={}, textChars={}",
                request.getUserId(), normalizedType, request.getSourceModule(), normalizedSourceText.length());
        MistakeItem existing = mistakeItemMapper.selectActiveBySource(
                request.getUserId(),
                normalizedType,
                normalizedSourceText
        );
        if (existing != null) {
            log.info("[误解本] active duplicate found userId={}, type={}, existingId={}",
                    request.getUserId(), normalizedType, existing.getId());
            return toResponse(existing);
        }

        MistakeItem item = new MistakeItem();
        item.setUserId(request.getUserId());
        item.setType(normalizedType);
        item.setSourceText(normalizedSourceText);
        item.setTranslation(resolveTranslation(request.getTranslation(), request.getSourceText(), request.getType()));
        item.setSourceModule(request.getSourceModule() == null || request.getSourceModule().isBlank() ? "阅读" : request.getSourceModule());
        item.setArticleKey(request.getArticleId());
        item.setStatus("active");
        mistakeItemMapper.insert(item);
        log.info("[误解本] inserted itemId={}, userId={}, type={}, articleKey={}",
                item.getId(), item.getUserId(), item.getType(), item.getArticleKey());
        return toResponse(item);
    }

    public List<MistakeItemResponse> listByType(Long userId, String type) {
        String normalizedType = normalizeType(type);
        List<MistakeItemResponse> result = mistakeItemMapper.selectActiveByUserAndType(userId, normalizedType).stream()
                .map(this::toResponse)
                .toList();
        log.info("[误解本] list active userId={}, type={}, count={}", userId, normalizedType, result.size());
        return result;
    }

    public MistakeReExplainResponse reExplain(Long userId, Long id) {
        log.info("[误解本] reExplain start userId={}, itemId={}", userId, id);
        MistakeItem item = mistakeItemMapper.selectOne(new LambdaQueryWrapper<MistakeItem>()
                .eq(MistakeItem::getId, id)
                .eq(MistakeItem::getUserId, userId)
                .eq(MistakeItem::getStatus, "active")
                .last("LIMIT 1"));
        if (item == null) {
            log.warn("[误解本] reExplain item not found userId={}, itemId={}", userId, id);
            throw new BizException(ErrorCode.MISTAKE_ITEM_NOT_FOUND);
        }

        tokenActionGuardService.guard(userId, "mistake-re-explain", Duration.ofSeconds(8));

        String translation = llmService.translateSelection(item.getSourceText(), normalizeType(item.getType()));
        if (translation == null || translation.isBlank() || "暂未获取翻译，请稍后重试".equals(translation)) {
            throw new BizException(ErrorCode.LLM_GENERATION_FAILED, "重新解释失败，请稍后再试");
        }

        item.setTranslation(translation.trim());
        mistakeItemMapper.updateById(item);
        log.info("[误解本] reExplain success userId={}, itemId={}, translationChars={}",
                userId, id, item.getTranslation().length());
        return MistakeReExplainResponse.builder()
                .id(item.getId())
                .translation(item.getTranslation())
                .build();
    }

    public void markDone(Long userId, Long id) {
        log.info("[误解本] markDone start userId={}, itemId={}", userId, id);
        MistakeItem item = mistakeItemMapper.selectOne(new LambdaQueryWrapper<MistakeItem>()
                .eq(MistakeItem::getId, id)
                .eq(MistakeItem::getUserId, userId)
                .eq(MistakeItem::getStatus, "active")
                .last("LIMIT 1"));
        if (item == null) {
            log.warn("[误解本] markDone item not found userId={}, itemId={}", userId, id);
            throw new BizException(ErrorCode.MISTAKE_ITEM_NOT_FOUND);
        }
        item.setStatus("done");
        mistakeItemMapper.updateById(item);
        log.info("[误解本] markDone success userId={}, itemId={}, type={}", userId, id, item.getType());
    }

    public List<String> listActiveMistakeWords(Long userId, int limit) {
        List<String> result = mistakeItemMapper.selectActiveWordMistakes(userId, limit).stream()
                .map(MistakeItem::getSourceText)
                .map(this::normalizeWord)
                .filter(word -> !word.isBlank())
                .distinct()
                .toList();
        log.info("[误解本] active mistake words userId={}, limit={}, returned={}", userId, limit, result.size());
        return result;
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
