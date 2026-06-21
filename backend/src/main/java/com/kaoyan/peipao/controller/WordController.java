package com.kaoyan.peipao.controller;

import com.kaoyan.peipao.common.Result;
import com.kaoyan.peipao.dto.request.CheckWordRequest;
import com.kaoyan.peipao.dto.response.WordVO;
import com.kaoyan.peipao.entity.Word;
import com.kaoyan.peipao.entity.WordProgress;
import com.kaoyan.peipao.service.WordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;

    /**
     * 获取一轮新词
     * GET /api/v1/words/new?userId=1&unit=1
     */
    @GetMapping("/new")
    public Result<List<WordVO>> getNewWords(@RequestParam Long userId, @RequestParam(defaultValue = "1") int unit) {
        List<Word> words = wordService.getNewWords(userId, unit);
        List<WordVO> vos = words.stream().map(w -> WordVO.builder()
                .id(w.getId()).word(w.getWord()).meaning(w.getMeaning())
                .unit(w.getUnit()).page(w.getPage()).build()).collect(Collectors.toList());
        return Result.ok(vos);
    }

    /**
     * 获取待复习词
     * GET /api/v1/words/review?userId=1
     */
    @GetMapping("/review")
    public Result<List<WordVO>> getReviewWords(@RequestParam Long userId) {
        List<WordProgress> dueList = wordService.getDueReviews(userId);
        // join with word table for display
        List<WordVO> vos = dueList.stream().map(wp -> {
            // Note: simplified - in real impl, batch-load words
            return WordVO.builder().id(wp.getWordId()).build();
        }).collect(Collectors.toList());
        return Result.ok(vos);
    }

    /**
     * 判断单词答案
     * POST /api/v1/words/check
     */
    @PostMapping("/check")
    public Result<Map<String, Object>> checkAnswer(@Valid @RequestBody CheckWordRequest req) {
        Map<String, Object> result;
        if (Boolean.TRUE.equals(req.getUnknown())) {
            result = wordService.markUnknown(req.getUserId(), req.getWordId());
        } else {
            result = wordService.checkAnswer(req.getUserId(), req.getWordId(), req.getUserAnswer());
        }
        return Result.ok(result);
    }

    /**
     * 获取词汇统计
     * GET /api/v1/words/stats?userId=1
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(@RequestParam Long userId) {
        return Result.ok(wordService.getStats(userId));
    }
}
