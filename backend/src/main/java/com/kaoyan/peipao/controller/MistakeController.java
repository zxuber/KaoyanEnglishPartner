package com.kaoyan.peipao.controller;

import com.kaoyan.peipao.common.Result;
import com.kaoyan.peipao.dto.request.AddMistakeItemRequest;
import com.kaoyan.peipao.dto.response.MistakeItemResponse;
import com.kaoyan.peipao.dto.response.MistakeReExplainResponse;
import com.kaoyan.peipao.service.MistakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mistakes")
@RequiredArgsConstructor
public class MistakeController {

    private final MistakeService mistakeService;

    @GetMapping
    public Result<List<MistakeItemResponse>> getMistakes(
            @RequestParam Long userId,
            @RequestParam String type
    ) {
        return Result.ok(mistakeService.listByType(userId, type));
    }

    @PostMapping
    public Result<MistakeItemResponse> addMistake(@Valid @RequestBody AddMistakeItemRequest request) {
        return Result.ok(mistakeService.addItem(request));
    }

    @PostMapping("/{id}/re-explain")
    public Result<MistakeReExplainResponse> reExplain(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        return Result.ok(mistakeService.reExplain(userId, id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> markDone(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        mistakeService.markDone(userId, id);
        return Result.ok(null);
    }
}
