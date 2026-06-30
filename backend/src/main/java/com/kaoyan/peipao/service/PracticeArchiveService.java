package com.kaoyan.peipao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kaoyan.peipao.common.BizException;
import com.kaoyan.peipao.common.ErrorCode;
import com.kaoyan.peipao.dto.response.PracticeArchiveItemResponse;
import com.kaoyan.peipao.dto.response.PracticeArchiveSummaryResponse;
import com.kaoyan.peipao.entity.PracticeRecord;
import com.kaoyan.peipao.mapper.PracticeRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeArchiveService {

    private final PracticeRecordMapper practiceRecordMapper;

    private static final List<String> MODULES = List.of("reading", "writing", "translation", "cloze");

    public void recordTraining(Long userId, String module, String questionKey, String title, String summary,
                               String selectedOption, String userAnswer, String coachReply, boolean completed) {
        String normalizedModule = normalizeModule(module);
        PracticeRecord record = practiceRecordMapper.selectOne(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, userId)
                .eq(PracticeRecord::getModule, normalizedModule)
                .eq(PracticeRecord::getQuestionKey, questionKey)
                .last("LIMIT 1"));
        boolean isNew = record == null;
        if (isNew) {
            record = new PracticeRecord();
            record.setUserId(userId);
            record.setModule(normalizedModule);
            record.setQuestionKey(questionKey);
            record.setImportant(0);
        }
        record.setTitle(title);
        record.setSummary(summary);
        record.setSelectedOption(selectedOption);
        record.setUserAnswer(userAnswer);
        record.setCoachReply(coachReply);
        record.setCompleted(completed ? 1 : 0);
        record.setTrainedAt(LocalDateTime.now());
        if (isNew) {
            practiceRecordMapper.insert(record);
        } else {
            practiceRecordMapper.updateById(record);
        }
        log.info("[训练档案] 记录训练 userId={}, module={}, questionKey={}, completed={}, newRecord={}",
                userId, normalizedModule, questionKey, completed, isNew);
    }

    public PracticeArchiveSummaryResponse summary(Long userId) {
        List<PracticeRecord> records = practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getUserId, userId));
        List<PracticeArchiveSummaryResponse.ModuleStat> modules = MODULES.stream()
                .map(module -> {
                    long completed = records.stream()
                            .filter(record -> module.equals(record.getModule()))
                            .filter(record -> Integer.valueOf(1).equals(record.getCompleted()))
                            .count();
                    long important = records.stream()
                            .filter(record -> module.equals(record.getModule()))
                            .filter(record -> Integer.valueOf(1).equals(record.getImportant()))
                            .count();
                    return PracticeArchiveSummaryResponse.ModuleStat.builder()
                            .module(module)
                            .moduleName(moduleName(module))
                            .completedCount((int) completed)
                            .importantCount((int) important)
                            .title(resolveTitle(module, (int) completed))
                            .subtitle("已训练完成 " + completed + unitName(module))
                            .build();
                })
                .toList();
        log.info("[训练档案] 汇总 userId={}, totalRecords={}", userId, records.size());
        return PracticeArchiveSummaryResponse.builder().modules(modules).build();
    }

    public List<PracticeArchiveItemResponse> list(Long userId, String module) {
        String normalizedModule = normalizeModule(module);
        List<PracticeArchiveItemResponse> result = practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                        .eq(PracticeRecord::getUserId, userId)
                        .eq(PracticeRecord::getModule, normalizedModule))
                .stream()
                .sorted(Comparator.comparing(PracticeRecord::getTrainedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
        log.info("[训练档案] 列表 userId={}, module={}, count={}", userId, normalizedModule, result.size());
        return result;
    }

    public PracticeArchiveItemResponse markImportant(Long userId, Long id, Boolean important) {
        PracticeRecord record = practiceRecordMapper.selectOne(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getId, id)
                .eq(PracticeRecord::getUserId, userId)
                .last("LIMIT 1"));
        if (record == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "训练记录不存在");
        }
        record.setImportant(Boolean.TRUE.equals(important) ? 1 : 0);
        practiceRecordMapper.updateById(record);
        log.info("[训练档案] 重要标记 userId={}, recordId={}, important={}", userId, id, record.getImportant());
        return toResponse(record);
    }

    private PracticeArchiveItemResponse toResponse(PracticeRecord record) {
        return PracticeArchiveItemResponse.builder()
                .id(record.getId())
                .module(record.getModule())
                .moduleName(moduleName(record.getModule()))
                .title(record.getTitle())
                .summary(record.getSummary())
                .selectedOption(record.getSelectedOption())
                .userAnswer(record.getUserAnswer())
                .coachReply(record.getCoachReply())
                .completed(Integer.valueOf(1).equals(record.getCompleted()))
                .important(Integer.valueOf(1).equals(record.getImportant()))
                .trainedAt(record.getTrainedAt() == null ? "" : record.getTrainedAt().toString())
                .build();
    }

    private String normalizeModule(String module) {
        if (module == null) return "writing";
        return switch (module) {
            case "reading", "writing", "translation", "cloze" -> module;
            default -> "writing";
        };
    }

    private String moduleName(String module) {
        return switch (module) {
            case "reading" -> "阅读";
            case "translation" -> "翻译";
            case "cloze" -> "完形填空";
            default -> "写作";
        };
    }

    private String unitName(String module) {
        return switch (module) {
            case "reading" -> "套题";
            case "translation" -> "句";
            case "cloze" -> "空";
            default -> "题";
        };
    }

    private String resolveTitle(String module, int count) {
        if ("reading".equals(module)) {
            if (count >= 50) return "阅读高频训练者";
            if (count >= 21) return "阅读训练稳定期";
            if (count >= 6) return "定位能力成长期";
            return "刚开始建立阅读手感";
        }
        if ("translation".equals(module)) {
            if (count >= 31) return "翻译稳定训练者";
            if (count >= 11) return "长难句拆解进阶";
            return "主干识别入门";
        }
        if ("cloze".equals(module)) {
            if (count >= 41) return "完形判断路径形成中";
            if (count >= 11) return "搭配与逻辑积累中";
            return "开始练选择依据";
        }
        if (count >= 21) return "写作结构感形成中";
        if (count >= 6) return "表达积累中";
        return "开始搭建写作骨架";
    }
}
