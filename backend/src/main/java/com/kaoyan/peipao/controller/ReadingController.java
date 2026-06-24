package com.kaoyan.peipao.controller;

import com.kaoyan.peipao.common.Result;
import com.kaoyan.peipao.dto.request.ReadingCoachRequest;
import com.kaoyan.peipao.dto.request.ReadingTranslateRequest;
import com.kaoyan.peipao.dto.response.ReadingArticleResponse;
import com.kaoyan.peipao.dto.response.ReadingTranslateResponse;
import com.kaoyan.peipao.service.ReadingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reading")
@RequiredArgsConstructor
public class ReadingController {

    private final ReadingService readingService;

    @GetMapping("/article")
    public Result<ReadingArticleResponse> getArticle(@RequestParam Long userId) {
        return Result.ok(readingService.getArticle(userId));
    }

    @PostMapping("/coach")
    public Result<Map<String, Object>> coach(@Valid @RequestBody ReadingCoachRequest request) {
        int turn = request.getTurn() == null ? 1 : request.getTurn();
        return Result.ok(readingService.coachAnswer(
                request.getUserId(),
                request.getArticleId(),
                request.getQuestionId(),
                request.getUserAnswer(),
                request.getSelectedOption(),
                turn
        ));
    }

    @PostMapping("/translate")
    public Result<ReadingTranslateResponse> translate(@Valid @RequestBody ReadingTranslateRequest request) {
        return Result.ok(readingService.translateSelection(
                request.getUserId(),
                request.getArticleId(),
                request.getContentType(),
                request.getSourceText()
        ));
    }
}
