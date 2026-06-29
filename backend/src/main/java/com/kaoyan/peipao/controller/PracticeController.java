package com.kaoyan.peipao.controller;

import com.kaoyan.peipao.common.Result;
import com.kaoyan.peipao.dto.request.PracticeCoachRequest;
import com.kaoyan.peipao.dto.response.ClozeTaskResponse;
import com.kaoyan.peipao.dto.response.PracticeCoachResponse;
import com.kaoyan.peipao.dto.response.TranslationTaskResponse;
import com.kaoyan.peipao.dto.response.WritingTaskResponse;
import com.kaoyan.peipao.service.PracticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    @GetMapping("/writing/task")
    public Result<WritingTaskResponse> getWritingTask(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "small") String type
    ) {
        return Result.ok(practiceService.getWritingTask(userId, type));
    }

    @PostMapping("/writing/coach")
    public Result<PracticeCoachResponse> coachWriting(@Valid @RequestBody PracticeCoachRequest request) {
        return Result.ok(practiceService.coachWriting(request));
    }

    @GetMapping("/translation/task")
    public Result<TranslationTaskResponse> getTranslationTask(@RequestParam Long userId) {
        return Result.ok(practiceService.getTranslationTask(userId));
    }

    @PostMapping("/translation/coach")
    public Result<PracticeCoachResponse> coachTranslation(@Valid @RequestBody PracticeCoachRequest request) {
        return Result.ok(practiceService.coachTranslation(request));
    }

    @GetMapping("/cloze/task")
    public Result<ClozeTaskResponse> getClozeTask(@RequestParam Long userId) {
        return Result.ok(practiceService.getClozeTask(userId));
    }

    @PostMapping("/cloze/coach")
    public Result<PracticeCoachResponse> coachCloze(@Valid @RequestBody PracticeCoachRequest request) {
        return Result.ok(practiceService.coachCloze(request));
    }
}
