package com.kaoyan.peipao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReadingService {

    private final ReadingArticleMapper readingArticleMapper;
    private final ReadingQuestionMapper readingQuestionMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final ReadingTranslationLogMapper readingTranslationLogMapper;
    private final LLMService llmService;

    public ReadingArticleResponse getArticle(Long userId) {
        ReadingArticle article = requireActiveArticle();
        List<ReadingQuestion> questions = readingQuestionMapper.selectByArticleId(article.getId());
        int usedCount = readingTranslationLogMapper.countByUserAndArticle(userId, article.getId());
        int limit = article.getTranslationLimit() == null ? 5 : article.getTranslationLimit();

        return ReadingArticleResponse.builder()
                .articleId(article.getArticleKey())
                .source(article.getSource())
                .title(article.getTitle())
                .passage(article.getPassage())
                .translationLimit(limit)
                .translationUsed(usedCount)
                .translationRemaining(Math.max(0, limit - usedCount))
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

        return Map.of(
                "coachReply", coachReply,
                "revealAnswer", reveal,
                "answer", reveal ? question.getAnswer() : "",
                "explanation", reveal ? question.getExplanation() : "",
                "turn", turn
        );
    }

    public ReadingTranslateResponse translateSelection(Long userId, String articleId, String contentType, String sourceText) {
        ReadingArticle article = requireArticle(articleId);
        int limit = article.getTranslationLimit() == null ? 5 : article.getTranslationLimit();
        int usedCount = readingTranslationLogMapper.countByUserAndArticle(userId, article.getId());
        if (usedCount >= limit) {
            throw new RuntimeException(ErrorCode.READING_TRANSLATION_LIMIT_REACHED.getMessage());
        }

        String translatedText = llmService.translateSelection(sourceText.trim(), contentType);

        ReadingTranslationLog log = new ReadingTranslationLog();
        log.setUserId(userId);
        log.setArticleId(article.getId());
        log.setContentType(normalizeContentType(contentType));
        log.setSourceText(sourceText.trim());
        log.setTranslatedText(translatedText);
        readingTranslationLogMapper.insert(log);

        int nextUsedCount = usedCount + 1;
        return ReadingTranslateResponse.builder()
                .translatedText(translatedText)
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
