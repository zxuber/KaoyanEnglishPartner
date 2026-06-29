package com.kaoyan.peipao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kaoyan.peipao.common.BizException;
import com.kaoyan.peipao.common.ErrorCode;
import com.kaoyan.peipao.dto.response.ReadingArticleResponse;
import com.kaoyan.peipao.dto.response.ReadingTranslateResponse;
import com.kaoyan.peipao.entity.ReadingArticle;
import com.kaoyan.peipao.entity.ReadingQuestion;
import com.kaoyan.peipao.entity.ReadingRecord;
import com.kaoyan.peipao.entity.ReadingTranslationLog;
import com.kaoyan.peipao.mapper.ReadingArticleMapper;
import com.kaoyan.peipao.mapper.ReadingQuestionMapper;
import com.kaoyan.peipao.mapper.ReadingRecordMapper;
import com.kaoyan.peipao.mapper.ReadingTranslationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingService {

    private final ReadingArticleMapper readingArticleMapper;
    private final ReadingQuestionMapper readingQuestionMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final ReadingTranslationLogMapper readingTranslationLogMapper;
    private final LLMService llmService;
    private final TranslationService translationService;
    private final TokenActionGuardService tokenActionGuardService;

    private static final int WORD_TRANSLATION_LIMIT = 5;
    private static final int SENTENCE_TRANSLATION_LIMIT = 3;

    public ReadingArticleResponse getArticle(Long userId) {
        ReadingArticle article = requireActiveArticle();
        List<ReadingQuestion> questions = readingQuestionMapper.selectByArticleId(article.getId());
        String readingSessionId = buildReadingSessionId();
        log.info("[阅读] article loaded userId={}, articleKey={}, articleDbId={}, questions={}, sessionId={}",
                userId, article.getArticleKey(), article.getId(), questions.size(), readingSessionId);

        return ReadingArticleResponse.builder()
                .articleId(article.getArticleKey())
                .source(article.getSource())
                .title(article.getTitle())
                .passage(article.getPassage())
                .readingSessionId(readingSessionId)
                .wordTranslationLimit(WORD_TRANSLATION_LIMIT)
                .wordTranslationUsed(0)
                .wordTranslationRemaining(WORD_TRANSLATION_LIMIT)
                .sentenceTranslationLimit(SENTENCE_TRANSLATION_LIMIT)
                .sentenceTranslationUsed(0)
                .sentenceTranslationRemaining(SENTENCE_TRANSLATION_LIMIT)
                .questions(questions.stream().map(this::toQuestionItem).toList())
                .build();
    }

    public Map<String, Object> coachAnswer(
            Long userId,
            String articleId,
            String questionId,
            String userAnswer,
            String selectedOption,
            int turn
    ) {
        ReadingArticle article = requireArticle(articleId);
        ReadingQuestion question = requireQuestion(questionId, article.getId());
        log.info("[阅读教练] start userId={}, articleId={}, questionId={}, selectedOption={}, turn={}, answerChars={}",
                userId,
                articleId,
                questionId,
                selectedOption,
                turn,
                userAnswer == null ? 0 : userAnswer.length());
        tokenActionGuardService.guard(userId, "reading-coach", Duration.ofSeconds(4));

        String coachReply = llmService.generateReadingCoachReply(
                article.getTitle(),
                article.getPassage(),
                question.getStem(),
                question.getFocus(),
                question.getAnswer(),
                question.getExplanation(),
                userAnswer,
                selectedOption,
                turn
        );

        boolean reveal = turn >= 2;

        ReadingRecord record = new ReadingRecord();
        record.setUserId(userId);
        record.setArticleId(article.getId());
        record.setQuestionId(question.getId());
        record.setSelectedOption(selectedOption);
        record.setUserReasoning(userAnswer);
        record.setCoachReply(coachReply);
        record.setTurn(turn);
        record.setRevealAnswer(reveal ? 1 : 0);
        readingRecordMapper.insert(record);
        log.info("[阅读教练] saved record userId={}, articleDbId={}, questionDbId={}, recordId={}, revealAnswer={}",
                userId, article.getId(), question.getId(), record.getId(), reveal);

        return Map.of(
                "coachReply", coachReply,
                "revealAnswer", reveal,
                "answer", reveal ? question.getAnswer() : "",
                "explanation", reveal ? question.getExplanation() : "",
                "turn", turn
        );
    }

    public ReadingTranslateResponse translateSelection(Long userId, String articleId, String readingSessionId, String contentType, String sourceText) {
        ReadingArticle article = requireArticle(articleId);
        String normalizedContentType = normalizeContentType(contentType);
        int limit = "sentence".equals(normalizedContentType) ? SENTENCE_TRANSLATION_LIMIT : WORD_TRANSLATION_LIMIT;
        int usedCount = readingTranslationLogMapper.countByUserAndArticleAndSessionAndType(
                userId,
                article.getId(),
                readingSessionId,
                normalizedContentType
        );
        log.info("[阅读翻译] request userId={}, articleId={}, sessionId={}, contentType={}, used={}/{}, textChars={}",
                userId,
                articleId,
                readingSessionId,
                normalizedContentType,
                usedCount,
                limit,
                sourceText == null ? 0 : sourceText.trim().length());
        if (usedCount >= limit) {
            log.warn("[阅读翻译] limit reached userId={}, articleId={}, sessionId={}, contentType={}, used={}, limit={}",
                    userId, articleId, readingSessionId, normalizedContentType, usedCount, limit);
            throw new BizException("sentence".equals(normalizedContentType)
                    ? ErrorCode.SENTENCE_TRANSLATION_LIMIT_REACHED
                    : ErrorCode.READING_TRANSLATION_LIMIT_REACHED);
        }
        if (translationService.shouldUseLlm(sourceText.trim(), normalizedContentType)) {
            log.info("[阅读翻译] LLM required userId={}, contentType={}", userId, normalizedContentType);
            tokenActionGuardService.guard(userId, "reading-translate-" + normalizedContentType, Duration.ofSeconds(4));
        } else {
            log.info("[阅读翻译] local dictionary can satisfy translation userId={}, contentType={}", userId, normalizedContentType);
        }

        String translatedText = translationService.translateSelection(sourceText.trim(), normalizedContentType);

        ReadingTranslationLog translationLog = new ReadingTranslationLog();
        translationLog.setUserId(userId);
        translationLog.setArticleId(article.getId());
        translationLog.setSessionId(readingSessionId);
        translationLog.setContentType(normalizedContentType);
        translationLog.setSourceText(sourceText.trim());
        translationLog.setTranslatedText(translatedText);
        readingTranslationLogMapper.insert(translationLog);
        log.info("[阅读翻译] saved logId={}, userId={}, articleDbId={}, contentType={}, nextUsed={}/{}",
                translationLog.getId(), userId, article.getId(), normalizedContentType, usedCount + 1, limit);

        int nextUsedCount = usedCount + 1;
        return ReadingTranslateResponse.builder()
                .translatedText(translatedText)
                .contentType(normalizedContentType)
                .limit(limit)
                .usedCount(nextUsedCount)
                .remainingCount(Math.max(0, limit - nextUsedCount))
                .build();
    }

    private ReadingArticle requireActiveArticle() {
        ReadingArticle article = readingArticleMapper.selectActiveArticle();
        if (article == null) {
            throw new RuntimeException(ErrorCode.READING_NOT_FOUND.getMessage());
        }
        return article;
    }

    private ReadingArticle requireArticle(String articleId) {
        ReadingArticle article = readingArticleMapper.selectByArticleKey(articleId);
        if (article == null) {
            throw new RuntimeException(ErrorCode.READING_NOT_FOUND.getMessage());
        }
        return article;
    }

    private ReadingQuestion requireQuestion(String questionId, Long articleDbId) {
        ReadingQuestion question;
        try {
            question = readingQuestionMapper.selectById(Long.parseLong(questionId));
        } catch (NumberFormatException e) {
            question = readingQuestionMapper.selectOne(new LambdaQueryWrapper<ReadingQuestion>()
                    .eq(ReadingQuestion::getArticleId, articleDbId)
                    .eq(ReadingQuestion::getQuestionNo, parseQuestionNo(questionId)));
        }
        if (question == null || !articleDbId.equals(question.getArticleId())) {
            throw new RuntimeException(ErrorCode.READING_QUESTION_NOT_FOUND.getMessage());
        }
        return question;
    }

    private int parseQuestionNo(String questionId) {
        String digits = questionId.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return -1;
        }
        return Integer.parseInt(digits);
    }

    private String normalizeContentType(String contentType) {
        return "sentence".equalsIgnoreCase(contentType) ? "sentence" : "word";
    }

    private String buildReadingSessionId() {
        return "reading-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private ReadingArticleResponse.QuestionItem toQuestionItem(ReadingQuestion question) {
        return ReadingArticleResponse.QuestionItem.builder()
                .id(String.valueOf(question.getId()))
                .stem(question.getStem())
                .focus(question.getFocus())
                .options(List.of(
                        ReadingArticleResponse.OptionItem.builder().label("A").content(question.getOptionA()).build(),
                        ReadingArticleResponse.OptionItem.builder().label("B").content(question.getOptionB()).build(),
                        ReadingArticleResponse.OptionItem.builder().label("C").content(question.getOptionC()).build(),
                        ReadingArticleResponse.OptionItem.builder().label("D").content(question.getOptionD()).build()
                ))
                .answer(question.getAnswer())
                .explanation(question.getExplanation())
                .build();
    }
}
