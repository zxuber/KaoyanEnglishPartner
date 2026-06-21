package com.kaoyan.peipao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kaoyan.peipao.entity.Word;
import com.kaoyan.peipao.entity.WordProgress;
import com.kaoyan.peipao.mapper.WordMapper;
import com.kaoyan.peipao.mapper.WordProgressMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WordService {

    private final WordMapper wordMapper;
    private final WordProgressMapper wpMapper;
    private final WordJudgeService judgeService;

    /** 每轮词数 */
    private static final int BATCH_SIZE = 20;
    /** 多少次连续正确后标记为 mastered */
    private static final int MASTERY_STREAK = 3;
    /** 艾宾浩斯间隔序列 */
    private static final int[] INTERVALS = {1, 2, 4, 7, 15};

    /**
     * 获取一轮新词（从指定 Unit 按序取未学过的词）
     */
    public List<Word> getNewWords(Long userId, int unit) {
        List<Word> unitWords = wordMapper.selectByUnit(unit);
        List<Word> newWords = new ArrayList<>();
        for (Word w : unitWords) {
            WordProgress wp = wpMapper.selectByUserAndWord(userId, w.getId());
            if (wp == null || "new".equals(wp.getStatus())) {
                newWords.add(w);
                if (newWords.size() >= BATCH_SIZE) break;
            }
        }
        return newWords;
    }

    /**
     * 获取待复习词（今天到期 + 未 mastered）
     */
    public List<WordProgress> getDueReviews(Long userId) {
        return wpMapper.selectDueReviews(userId, BATCH_SIZE);
    }

    /**
     * 判断单词答案并更新进度
     */
    public Map<String, Object> checkAnswer(Long userId, Long wordId, String userAnswer) {
        log.info("[Word] checkAnswer: userId={}, wordId={}, answer=[{}]", userId, wordId, userAnswer);
        Word word = wordMapper.selectById(wordId);
        if (word == null) throw new RuntimeException("word not found");

        boolean correct = judgeService.judge(word.getMeaning(), userAnswer);
        log.info("[Word] judge: word={}, meaning={}, answer={}, correct={}", word.getWord(), word.getMeaning(), userAnswer, correct);

        WordProgress wp = wpMapper.selectByUserAndWord(userId, wordId);
        if (wp == null) {
            wp = new WordProgress();
            wp.setUserId(userId);
            wp.setWordId(wordId);
            wp.setStatus("learning");
            wp.setMistakeCount(0);
            wp.setCorrectStreak(0);
            wp.setReviewInterval(0);
        }

        if (correct) {
            int streak = wp.getCorrectStreak() + 1;
            wp.setCorrectStreak(streak);
            wp.setReviewInterval(getNextInterval(wp.getReviewInterval()));

            if (streak >= MASTERY_STREAK) {
                wp.setStatus("mastered");
                wp.setNextReviewDate(null);
            } else {
                wp.setNextReviewDate(LocalDate.now().plusDays(wp.getReviewInterval()));
            }
        } else {
            wp.setCorrectStreak(0);
            wp.setMistakeCount(wp.getMistakeCount() + 1);
            wp.setReviewInterval(1);
            wp.setNextReviewDate(LocalDate.now().plusDays(1));
            if ("mastered".equals(wp.getStatus())) {
                wp.setStatus("learning");
            }
        }
        wp.setLastReviewedAt(LocalDateTime.now());

        if (wp.getId() == null) {
            wpMapper.insert(wp);
        } else {
            wpMapper.updateById(wp);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("correct", correct);
        result.put("wordId", wordId);
        result.put("meaning", correct ? null : word.getMeaning());
        result.put("status", wp.getStatus());
        result.put("streak", wp.getCorrectStreak());
        return result;
    }

    /**
     * 标记不认识
     */
    public Map<String, Object> markUnknown(Long userId, Long wordId) {
        return checkAnswer(userId, wordId, "");
    }

    /**
     * 获取用户词汇统计
     */
    public Map<String, Object> getStats(Long userId) {
        int mastered = wpMapper.countMastered(userId);
        int total = wpMapper.countTotal(userId);
        Map<String, Object> stats = new HashMap<>();
        stats.put("mastered", mastered);
        stats.put("total", total);
        stats.put("vocabulary", Math.min(total * 3, 6547)); // rough estimate
        return stats;
    }

    private int getNextInterval(int current) {
        if (current == 0) return INTERVALS[0];
        for (int i = 0; i < INTERVALS.length - 1; i++) {
            if (INTERVALS[i] == current) return INTERVALS[i + 1];
        }
        return INTERVALS[INTERVALS.length - 1];
    }
}
