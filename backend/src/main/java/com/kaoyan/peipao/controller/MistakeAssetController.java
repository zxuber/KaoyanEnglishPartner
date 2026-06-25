package com.kaoyan.peipao.controller;

import com.kaoyan.peipao.common.Result;
import com.kaoyan.peipao.dto.response.MistakeAssetResponse;
import com.kaoyan.peipao.service.MistakeAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mistake-assets")
@RequiredArgsConstructor
public class MistakeAssetController {

    private final MistakeAssetService mistakeAssetService;

    @GetMapping
    public Result<List<MistakeAssetResponse>> listByCategory(
            @RequestParam Long userId,
            @RequestParam String category
    ) {
        return Result.ok(mistakeAssetService.listByCategory(userId, category));
    }

    @DeleteMapping("/{id}")
    public Result<Void> markDone(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        mistakeAssetService.markDone(userId, id);
        return Result.ok(null);
    }
}
