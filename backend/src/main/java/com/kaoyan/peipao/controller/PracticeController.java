package com.kaoyan.peipao.controller;

import com.kaoyan.peipao.common.Result;
import com.kaoyan.peipao.dto.request.PracticeCoachRequest;
import com.kaoyan.peipao.dto.response.ClozeTaskResponse;
import com.kaoyan.peipao.dto.response.PracticeArchiveItemResponse;
import com.kaoyan.peipao.dto.response.PracticeArchiveSummaryResponse;
import com.kaoyan.peipao.dto.response.PracticeCoachResponse;
import com.kaoyan.peipao.dto.response.TranslationTaskResponse;
import com.kaoyan.peipao.dto.response.WritingTaskResponse;
import com.kaoyan.peipao.service.PracticeArchiveService;
import com.kaoyan.peipao.service.PracticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;
    private final PracticeArchiveService practiceArchiveService;

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

    @GetMapping("/archive/summary")
    public Result<PracticeArchiveSummaryResponse> archiveSummary(@RequestParam Long userId) {
        return Result.ok(practiceArchiveService.summary(userId));
    }

    @GetMapping("/archive")
    public Result<List<PracticeArchiveItemResponse>> archiveList(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "writing") String module
    ) {
        return Result.ok(practiceArchiveService.list(userId, module));
    }

    @PostMapping("/archive/{id}/important")
    public Result<PracticeArchiveItemResponse> markImportant(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "true") Boolean important
    ) {
        return Result.ok(practiceArchiveService.markImportant(userId, id, important));
    }
}
