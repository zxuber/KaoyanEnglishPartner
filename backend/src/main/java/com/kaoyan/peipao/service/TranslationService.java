package com.kaoyan.peipao.service;

import com.kaoyan.peipao.entity.Word;
import com.kaoyan.peipao.mapper.WordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationService {

    private static final Map<String, String> HIGH_FREQUENCY_WORDS = new LinkedHashMap<>();

    static {
        HIGH_FREQUENCY_WORDS.put("and", "和；并且");
        HIGH_FREQUENCY_WORDS.put("or", "或者；还是");
        HIGH_FREQUENCY_WORDS.put("but", "但是；而");
        HIGH_FREQUENCY_WORDS.put("if", "如果；是否");
        HIGH_FREQUENCY_WORDS.put("you", "你；你们");
        HIGH_FREQUENCY_WORDS.put("we", "我们");
        HIGH_FREQUENCY_WORDS.put("they", "他们；她们；它们");
        HIGH_FREQUENCY_WORDS.put("he", "他");
        HIGH_FREQUENCY_WORDS.put("she", "她");
        HIGH_FREQUENCY_WORDS.put("it", "它；这");
        HIGH_FREQUENCY_WORDS.put("the", "这；该");
        HIGH_FREQUENCY_WORDS.put("a", "一个");
        HIGH_FREQUENCY_WORDS.put("an", "一个");
        HIGH_FREQUENCY_WORDS.put("to", "到；去；向");
        HIGH_FREQUENCY_WORDS.put("of", "的；关于");
        HIGH_FREQUENCY_WORDS.put("in", "在……里；在……中");
        HIGH_FREQUENCY_WORDS.put("on", "在……上；关于");
        HIGH_FREQUENCY_WORDS.put("for", "为了；对于");
        HIGH_FREQUENCY_WORDS.put("with", "和；带有；用");
        HIGH_FREQUENCY_WORDS.put("as", "作为；像；当……时");
        HIGH_FREQUENCY_WORDS.put("at", "在；以；朝");
        HIGH_FREQUENCY_WORDS.put("by", "通过；被；在……旁");
        HIGH_FREQUENCY_WORDS.put("from", "从；来自");
        HIGH_FREQUENCY_WORDS.put("is", "是");
        HIGH_FREQUENCY_WORDS.put("are", "是");
        HIGH_FREQUENCY_WORDS.put("was", "是；曾是");
        HIGH_FREQUENCY_WORDS.put("were", "是；曾是");
        HIGH_FREQUENCY_WORDS.put("be", "是；成为");
        HIGH_FREQUENCY_WORDS.put("been", "已经是；曾经是");
        HIGH_FREQUENCY_WORDS.put("being", "正在是；存在");
        HIGH_FREQUENCY_WORDS.put("do", "做；确实");
        HIGH_FREQUENCY_WORDS.put("does", "做；的确");
        HIGH_FREQUENCY_WORDS.put("did", "做了");
        HIGH_FREQUENCY_WORDS.put("have", "有；已经");
        HIGH_FREQUENCY_WORDS.put("has", "有；已经");
        HIGH_FREQUENCY_WORDS.put("had", "有过；已经");
        HIGH_FREQUENCY_WORDS.put("not", "不；不是");
        HIGH_FREQUENCY_WORDS.put("this", "这；这个");
        HIGH_FREQUENCY_WORDS.put("that", "那；那个；引导从句");
        HIGH_FREQUENCY_WORDS.put("these", "这些");
        HIGH_FREQUENCY_WORDS.put("those", "那些");
        HIGH_FREQUENCY_WORDS.put("can", "能；可以");
        HIGH_FREQUENCY_WORDS.put("could", "能够；可能；可以");
        HIGH_FREQUENCY_WORDS.put("will", "将；会；意愿");
        HIGH_FREQUENCY_WORDS.put("would", "将会；愿意；会");
        HIGH_FREQUENCY_WORDS.put("may", "可能；可以");
        HIGH_FREQUENCY_WORDS.put("might", "可能");
        HIGH_FREQUENCY_WORDS.put("should", "应该");
        HIGH_FREQUENCY_WORDS.put("than", "比；相比");
        HIGH_FREQUENCY_WORDS.put("then", "然后；那么");
        HIGH_FREQUENCY_WORDS.put("there", "那里；存在");
        HIGH_FREQUENCY_WORDS.put("their", "他们的；她们的；它们的");
        HIGH_FREQUENCY_WORDS.put("our", "我们的");
        HIGH_FREQUENCY_WORDS.put("your", "你的；你们的");
    }

    private final WordMapper wordMapper;
    private final LLMService llmService;

    public boolean shouldUseLlm(String sourceText, String contentType) {
        if ("sentence".equalsIgnoreCase(contentType)) {
            log.info("[翻译路由] sentence translation requires LLM textChars={}", sourceText == null ? 0 : sourceText.length());
            return true;
        }

        String normalized = normalizeWord(sourceText);
        if (normalized.isBlank()) {
            log.info("[翻译路由] blank word after normalize, fallback to LLM rawText={}", sourceText);
            return true;
        }

        for (String candidate : buildLookupCandidates(normalized)) {
            if (HIGH_FREQUENCY_WORDS.containsKey(candidate)) {
                log.info("[翻译路由] high-frequency dictionary hit candidate={}", candidate);
                return false;
            }
            Word word = wordMapper.selectByWord(candidate);
            if (word != null && word.getMeaning() != null && !word.getMeaning().isBlank()) {
                log.info("[翻译路由] word table hit candidate={}, wordId={}", candidate, word.getId());
                return false;
            }
        }
        log.info("[翻译路由] no local hit, fallback to LLM normalized={}", normalized);
        return true;
    }

    public String translateSelection(String sourceText, String contentType) {
        if ("sentence".equalsIgnoreCase(contentType)) {
            log.info("[翻译] sentence via LLM textChars={}", sourceText == null ? 0 : sourceText.length());
            return llmService.translateSelection(sourceText, contentType);
        }

        String normalized = normalizeWord(sourceText);
        if (normalized.isBlank()) {
            log.info("[翻译] blank normalized word, using LLM rawText={}", sourceText);
            return llmService.translateSelection(sourceText, contentType);
        }

        for (String candidate : buildLookupCandidates(normalized)) {
            String localTranslation = HIGH_FREQUENCY_WORDS.get(candidate);
            if (localTranslation != null) {
                log.info("[翻译] high-frequency dictionary result candidate={}", candidate);
                return localTranslation;
            }

            Word word = wordMapper.selectByWord(candidate);
            if (word != null && word.getMeaning() != null && !word.getMeaning().isBlank()) {
                log.info("[翻译] word table result candidate={}, wordId={}", candidate, word.getId());
                return simplifyMeaning(word.getMeaning());
            }
        }

        log.info("[翻译] LLM fallback normalized={}", normalized);
        return llmService.translateSelection(sourceText, contentType);
    }

    private String normalizeWord(String sourceText) {
        return sourceText == null
                ? ""
                : sourceText.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("^[^a-z]+|[^a-z'-]+$", "");
    }

    private Iterable<String> buildLookupCandidates(String normalized) {
        LinkedHashMap<String, Boolean> candidates = new LinkedHashMap<>();
        candidates.put(normalized, true);

        if (normalized.endsWith("ly") && normalized.length() > 4) {
            candidates.put(normalized.substring(0, normalized.length() - 2), true);
        }
        if (normalized.endsWith("ies") && normalized.length() > 4) {
            candidates.put(normalized.substring(0, normalized.length() - 3) + "y", true);
        }
        if (normalized.endsWith("es") && normalized.length() > 3) {
            candidates.put(normalized.substring(0, normalized.length() - 2), true);
        }
        if (normalized.endsWith("s") && normalized.length() > 3) {
            candidates.put(normalized.substring(0, normalized.length() - 1), true);
        }
        if (normalized.endsWith("ied") && normalized.length() > 4) {
            candidates.put(normalized.substring(0, normalized.length() - 3) + "y", true);
        }
        if (normalized.endsWith("ed") && normalized.length() > 4) {
            candidates.put(normalized.substring(0, normalized.length() - 2), true);
        }
        if (normalized.endsWith("ing") && normalized.length() > 5) {
            candidates.put(normalized.substring(0, normalized.length() - 3), true);
        }
        if (normalized.endsWith("er") && normalized.length() > 4) {
            candidates.put(normalized.substring(0, normalized.length() - 2), true);
        }
        if (normalized.endsWith("est") && normalized.length() > 5) {
            candidates.put(normalized.substring(0, normalized.length() - 3), true);
        }

        return candidates.keySet();
    }

    private String simplifyMeaning(String meaning) {
        String normalized = meaning.replaceAll("\\s+", "");
        String[] parts = normalized.split("[；;]");
        if (parts.length == 0) {
            return normalized;
        }
        if (parts.length == 1) {
            return parts[0];
        }
        return parts[0] + "；" + parts[1];
    }
}
