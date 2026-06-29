package com.kaoyan.peipao.controller;

import com.kaoyan.peipao.common.Result;
import com.kaoyan.peipao.service.SpeechService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/speech")
@RequiredArgsConstructor
public class SpeechController {

    private final SpeechService speechService;

    @PostMapping("/recognize")
    public Result<Map<String, Object>> recognize(@RequestParam("audio") MultipartFile audio) {
        log.info("[语音识别] receive upload: fileName={}, size={}bytes", audio.getOriginalFilename(), audio.getSize());
        Map<String, Object> result = new HashMap<>();

        if (audio.isEmpty()) {
            log.warn("[语音识别] empty file uploaded");
            result.put("text", "");
            result.put("success", false);
            result.put("error", "Empty audio file");
            return Result.ok(result);
        }

        Path tempFile = null;
        try {
            // Save to temp file
            String suffix = ".wav";
            String originalName = audio.getOriginalFilename();
            if (originalName != null && originalName.contains(".")) {
                suffix = originalName.substring(originalName.lastIndexOf("."));
            }
            tempFile = Files.createTempFile("stt_", suffix);
            audio.transferTo(tempFile.toFile());

            // Transcribe
            String text = speechService.transcribe(tempFile.toFile());
            log.info("[语音识别] recognition done: [{}]", text);

            result.put("text", text);
            result.put("success", text != null && !text.isEmpty());
            return Result.ok(result);
        } catch (IOException e) {
            log.error("[语音识别] IO error", e);
            result.put("text", "");
            result.put("success", false);
            result.put("error", e.getMessage());
            return Result.ok(result);
        } finally {
            if (tempFile != null) {
                try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
            }
        }
    }
}