package com.kaoyan.peipao.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SpeechService {

    @Value("${app.stt.python-path:python}")
    private String pythonPath;

    @Value("${app.stt.script-path:scripts/stt.py}")
    private String scriptPath;

    @Value("${app.stt.timeout-seconds:10}")
    private int timeoutSeconds;

    @Value("${app.stt.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    /**
     * Transcribe audio file to text. Accepts any format (WAV, WebM, etc.),
     * auto-converts to 16kHz mono WAV via ffmpeg if needed, then calls Python STT.
     */
    public String transcribe(File audioFile) {
        log.info("[Speech] start: audio={}, size={}bytes", audioFile.getAbsolutePath(), audioFile.length());
        File effectiveFile = audioFile;

        if (!isWav(audioFile)) {
            log.info("[Speech] non-WAV detected, converting via ffmpeg...");
            File converted = convertToWav(audioFile);
            if (converted == null) {
                log.warn("[Speech] ffmpeg conversion failed, trying raw file");
            } else {
                effectiveFile = converted;
                log.info("[Speech] converted to WAV: {}bytes", effectiveFile.length());
            }
        }

        try {
            Path scriptAbsolute = resolveScriptPath();

            ProcessBuilder pb = new ProcessBuilder(
                    pythonPath,
                    scriptAbsolute.toAbsolutePath().toString(),
                    effectiveFile.getAbsolutePath()
            );
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.redirectErrorStream(true);
            log.info("[Speech] launching python: {} {} {}",
                    pythonPath, scriptAbsolute, effectiveFile.getAbsolutePath());

            Process process = pb.start();
            log.info("[Speech] python pid={}", process.pid());
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                log.warn("[Speech] python timed out after {}s", timeoutSeconds);
                return "";
            }

            // Read all output (stdout+stderr merged)
            String allOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            log.info("[Speech] raw output ({} chars): {}", allOutput.length(), allOutput);
            String text = "";
            // stt.py logs: [stt.py] Baidu ASR result: "TEXT"
            for (String line : allOutput.split("\n")) {
                if (line.contains("Baidu ASR result:")) {
                    int start = line.indexOf('"');
                    int end = line.lastIndexOf('"');
                    if (start >= 0 && end > start) {
                        text = line.substring(start + 1, end);
                    }
                    break;
                }
            }
            // Fallback: try stdout-only (last non-log line)
            if (text.isEmpty()) {
                for (int i = allOutput.split("\n").length - 1; i >= 0; i--) {
                    String line = allOutput.split("\n")[i].trim();
                    if (!line.isEmpty() && !line.startsWith("[stt.py]")) {
                        text = line;
                        break;
                    }
                }
            }
            if (process.exitValue() == 0) {
                log.info("[Speech] result: [{}]", text);
                return text;
            } else {
                log.warn("[Speech] python exit {}", process.exitValue());
                return "";
            }
        } catch (Exception e) {
            log.error("[Speech] failed", e);
            return "";
        } finally {
            if (effectiveFile != audioFile) {
                try { Files.deleteIfExists(effectiveFile.toPath()); } catch (IOException ignored) {}
            }
        }
    }

    private boolean isWav(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] h = new byte[4];
            if (fis.read(h) != 4) return false;
            return h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F';
        } catch (IOException e) {
            return false;
        }
    }

    private File convertToWav(File source) {
        try {
            Path out = Files.createTempFile("stt_conv_", ".wav");
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-y", "-i", source.getAbsolutePath(),
                    "-ar", "16000", "-ac", "1", "-sample_fmt", "s16", "-f", "wav",
                    out.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            log.info("[Speech] ffmpeg: {}", String.join(" ", pb.command()));
            Process p = pb.start();
            if (!p.waitFor(15, TimeUnit.SECONDS)) { p.destroyForcibly(); return null; }
            if (p.exitValue() != 0) {
                log.warn("[Speech] ffmpeg exit {}: {}",
                        p.exitValue(), new String(p.getInputStream().readAllBytes()).trim());
                return null;
            }
            log.info("[Speech] ffmpeg ok");
            return out.toFile();
        } catch (Exception e) {
            log.error("[Speech] ffmpeg error", e);
            return null;
        }
    }

    private Path resolveScriptPath() {
        Path p = Paths.get(scriptPath);
        if (Files.exists(p)) return p;
        p = Paths.get("backend", scriptPath);
        if (Files.exists(p)) return p;
        return Paths.get(scriptPath);
    }
}
