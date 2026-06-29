package com.kaoyan.peipao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kaoyan.peipao.dto.request.OnboardingRequest;
import com.kaoyan.peipao.dto.request.UpdateRecentTrainingRequest;
import com.kaoyan.peipao.dto.response.DashboardResponse;
import com.kaoyan.peipao.dto.response.PlanResponse;
import com.kaoyan.peipao.entity.User;
import com.kaoyan.peipao.mapper.UserMapper;
import com.kaoyan.peipao.mapper.WordProgressMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final WordProgressMapper wordProgressMapper;
    private final LLMService llmService;
    private final ObjectMapper objectMapper;
    private final TokenActionGuardService tokenActionGuardService;

    public Map<String, Object> submitOnboarding(OnboardingRequest request) {
        Long guardUserId = request.getUserId() == null ? 0L : request.getUserId();
        log.info("[用户] submitOnboarding start userId={}, examType={}, targetScore={}, remainingDays={}, weakModules={}",
                request.getUserId(), request.getExamType(), request.getTargetScore(), request.getRemainingDays(), request.getWeakModules());
        tokenActionGuardService.guard(guardUserId, "onboarding-generate-plan", Duration.ofSeconds(15));
        Map<String, Object> profile = new HashMap<>();
        profile.put("examType", request.getExamType());
        profile.put("targetScore", request.getTargetScore());
        profile.put("remainingDays", request.getRemainingDays());
        profile.put("currentScoreBand", request.getCurrentScoreBand());
        profile.put("englishLevel", request.getEnglishLevel());
        profile.put("weakModules", request.getWeakModules());
        profile.put("weekdayHours", request.getWeekdayHours());
        profile.put("weekendHours", request.getWeekendHours());
        profile.put("studyTimeSlot", request.getStudyTimeSlot());
        profile.put("planStyle", request.getPlanStyle());
        profile.put("biggestObstacle", request.getBiggestObstacle());
        profile.put("materials", request.getMaterials() != null ? request.getMaterials() : "无");

        log.info("[学习方案] 开始生成计划 target={}, days={}", request.getTargetScore(), request.getRemainingDays());
        PlanResponse plan = llmService.generatePlan(profile);

        User user = resolveUserForOnboarding(request.getUserId());
        applyOnboarding(user, request, plan);
        log.info("[用户] 学习画像已保存 id={}, onboardingDone=true", user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("plan", plan);
        result.put("userId", user.getId());
        return result;
    }

    public DashboardResponse getDashboard(Long userId) {
        log.info("[首页] loading dashboard userId={}", userId);
        User user = requireUser(userId);
        int masteredWords = wordProgressMapper.countMastered(userId);
        int totalWords = wordProgressMapper.countTotal(userId);
        int dueReviewCount = wordProgressMapper.selectDueReviews(userId, 100).size();
        log.info("[首页] stats userId={}, masteredWords={}, totalWords={}, dueReviewCount={}, totalCheckins={}",
                userId, masteredWords, totalWords, dueReviewCount, user.getTotalCheckins());

        DashboardResponse.ContinueTraining continueTraining = buildContinueTraining(user);
        List<DashboardResponse.TaskCard> todayTasks = buildTodayTasks(user, dueReviewCount);
        List<DashboardResponse.QuickEntry> quickEntries = List.of(
                DashboardResponse.QuickEntry.builder().title("单词").subtitle("主动回忆 20 题").page("/pages/word/index").accent("#0f766e").build(),
                DashboardResponse.QuickEntry.builder().title("阅读").subtitle("定位与排错训练").page("/pages/reading/index").accent("#b45309").build(),
                DashboardResponse.QuickEntry.builder().title("作文").subtitle("审题与提纲输出").page("/pages/writing/index").accent("#7c3aed").build(),
                DashboardResponse.QuickEntry.builder().title("模考").subtitle("阶段性套卷检查").page("/pages/exam/index").accent("#1d4ed8").build(),
                DashboardResponse.QuickEntry.builder().title("误解本").subtitle("集中清理薄弱点").page("/pages/mistake/index").accent("#be123c").build()
        );

        List<DashboardResponse.ReviewItem> reviewItems = List.of(
                DashboardResponse.ReviewItem.builder()
                        .label("待回炉词")
                        .value(String.valueOf(dueReviewCount))
                        .hint(dueReviewCount > 0 ? "先把遗忘风险最高的词过一遍" : "今天词汇回炉压力不大")
                        .build(),
                DashboardResponse.ReviewItem.builder()
                        .label("阅读提示")
                        .value(hasWeakModule(user, "阅读") ? "定位优先" : "保持节奏")
                        .hint(hasWeakModule(user, "阅读") ? "做题先找题干关键词，再回原文定位" : "保持每日一篇或隔日一篇")
                        .build(),
                DashboardResponse.ReviewItem.builder()
                        .label("执行风险")
                        .value(defaultText(user.getBiggestObstacle(), "待识别"))
                        .hint(buildRiskHint(user))
                        .build()
        );

        return DashboardResponse.builder()
                .greeting(buildGreeting(user))
                .onboardingDone(user.getPlanJson() != null && !user.getPlanJson().isBlank())
                .continueTraining(continueTraining)
                .todayTasks(todayTasks)
                .quickEntries(quickEntries)
                .reviewItems(reviewItems)
                .stats(DashboardResponse.Stats.builder()
                        .masteredWords(masteredWords)
                        .totalCheckins(user.getTotalCheckins() == null ? 0 : user.getTotalCheckins())
                        .targetScore(user.getTargetScore())
                        .currentPhase(defaultText(user.getCurrentPhase(), "phase1"))
                        .build())
                .build();
    }

    public void checkin(Long userId) {
        User user = requireUser(userId);
        LocalDate today = LocalDate.now();
        if (today.equals(user.getLastCheckinDate())) {
            log.info("[打卡] skipped duplicate checkin userId={}, date={}", userId, today);
            return;
        }
        user.setLastCheckinDate(today);
        user.setTotalCheckins((user.getTotalCheckins() == null ? 0 : user.getTotalCheckins()) + 1);
        userMapper.updateById(user);
        log.info("[打卡] success userId={}, date={}, totalCheckins={}", userId, today, user.getTotalCheckins());
    }

    public void updateRecentTraining(Long userId, UpdateRecentTrainingRequest request) {
        User user = requireUser(userId);
        try {
            log.info("[最近训练] update userId={}, module={}, title={}, page={}, progress={}/{}",
                    userId,
                    request.getModule(),
                    request.getTitle(),
                    request.getPage(),
                    request.getProgressCurrent(),
                    request.getProgressTotal());
            Map<String, Object> payload = new HashMap<>();
            payload.put("module", request.getModule());
            payload.put("title", request.getTitle());
            payload.put("subtitle", request.getSubtitle());
            payload.put("page", request.getPage());
            payload.put("progressCurrent", request.getProgressCurrent());
            payload.put("progressTotal", request.getProgressTotal());
            payload.put("accent", request.getAccent());
            user.setSessionId(user.getSessionId() == null || user.getSessionId().isBlank()
                    ? UUID.randomUUID().toString().replace("-", "").substring(0, 16)
                    : user.getSessionId());
            user.setNickname(user.getNickname());
            user.setAvatarUrl(user.getAvatarUrl());
            user.setMaterials(user.getMaterials());
            user.setWeakModules(user.getWeakModules());
            user.setPlanJson(mergeRecentTraining(user.getPlanJson(), payload));
            userMapper.updateById(user);
            log.info("[最近训练] saved userId={}, module={}, page={}", userId, request.getModule(), request.getPage());
        } catch (Exception e) {
            log.error("[最近训练] save failed userId={}", userId, e);
            throw new RuntimeException("保存最近训练失败", e);
        }
    }

    public PlanResponse getPlan(Long userId) {
        log.info("[学习方案] getPlan userId={}", userId);
        User user = userMapper.selectById(userId);
        if (user == null || user.getPlanJson() == null) {
            log.warn("[学习方案] missing plan userId={}", userId);
            throw new RuntimeException("方案不存在");
        }
        try {
            return objectMapper.readValue(stripRecentTraining(user.getPlanJson()), PlanResponse.class);
        } catch (Exception e) {
            log.error("[学习方案] parse failed userId={}", userId, e);
            throw new RuntimeException("方案数据解析失败", e);
        }
    }

    private User resolveUserForOnboarding(Long userId) {
        if (userId != null) {
            User existing = userMapper.selectById(userId);
            if (existing != null) {
                log.info("[用户] onboarding will update existing userId={}", userId);
                return existing;
            }
            log.warn("[用户] onboarding userId={} not found, will create fallback anonymous user", userId);
        }
        User user = new User();
        user.setSessionId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        user.setCurrentPhase("phase1");
        user.setPhaseStartDay(1);
        user.setTotalCheckins(0);
        userMapper.insert(user);
        log.info("[用户] created anonymous onboarding userId={}", user.getId());
        return user;
    }

    private void applyOnboarding(User user, OnboardingRequest request, PlanResponse plan) {
        user.setExamType(request.getExamType());
        user.setTargetScore(request.getTargetScore());
        user.setRemainingDays(request.getRemainingDays());
        user.setCurrentScoreBand(request.getCurrentScoreBand());
        user.setEnglishLevel(request.getEnglishLevel());
        user.setWeekdayHours(request.getWeekdayHours());
        user.setWeekendHours(request.getWeekendHours());
        user.setStudyTimeSlot(request.getStudyTimeSlot());
        user.setPlanStyle(request.getPlanStyle());
        user.setBiggestObstacle(request.getBiggestObstacle());
        try {
            user.setWeakModules(objectMapper.writeValueAsString(request.getWeakModules()));
        } catch (Exception e) {
            user.setWeakModules("[]");
        }
        try {
            user.setMaterials(objectMapper.writeValueAsString(request.getMaterials()));
        } catch (Exception e) {
            user.setMaterials("[]");
        }
        try {
            user.setPlanJson(objectMapper.writeValueAsString(plan));
        } catch (Exception e) {
            user.setPlanJson("{}");
        }
        user.setPlanGeneratedAt(LocalDateTime.now());
        user.setCurrentPhase(user.getCurrentPhase() == null ? "phase1" : user.getCurrentPhase());
        user.setPhaseStartDay(user.getPhaseStartDay() == null ? 1 : user.getPhaseStartDay());
        userMapper.updateById(user);
    }

    private DashboardResponse.ContinueTraining buildContinueTraining(User user) {
        try {
            Map<?, ?> root = objectMapper.readValue(user.getPlanJson(), Map.class);
            Object recentObj = root.get("_recentTraining");
            if (recentObj instanceof Map<?, ?> recent) {
                return DashboardResponse.ContinueTraining.builder()
                        .module(defaultText((String) recent.get("module"), "继续训练"))
                        .title(defaultText((String) recent.get("title"), "回到上次未完成的训练"))
                        .subtitle((String) recent.get("subtitle"))
                        .page(defaultText((String) recent.get("page"), "/pages/word/index"))
                        .progressCurrent(toInteger(recent.get("progressCurrent")))
                        .progressTotal(toInteger(recent.get("progressTotal")))
                        .accent(defaultText((String) recent.get("accent"), "#0f766e"))
                        .build();
            }
        } catch (Exception ignored) {
        }

        return DashboardResponse.ContinueTraining.builder()
                .module("推荐起点")
                .title(hasWeakModule(user, "词汇") ? "继续单词主动回忆" : "开始今天的第一组训练")
                .subtitle(hasWeakModule(user, "阅读") ? "先稳住词汇，再拉阅读正确率" : "按系统建议把第一项任务先做完")
                .page("/pages/word/index")
                .progressCurrent(0)
                .progressTotal(20)
                .accent("#0f766e")
                .build();
    }

    private List<DashboardResponse.TaskCard> buildTodayTasks(User user, int dueReviewCount) {
        List<DashboardResponse.TaskCard> tasks = new ArrayList<>();
        tasks.add(DashboardResponse.TaskCard.builder()
                .title(dueReviewCount > 0 ? "先清理回炉词" : "单词主动回忆 20 题")
                .subtitle(dueReviewCount > 0 ? "优先处理遗忘风险最高的词" : "用口头回忆而不是被动浏览")
                .reason(hasWeakModule(user, "词汇") ? "你的画像里明确提到了词汇短板" : "主动输出是今天效率最高的热启动")
                .page("/pages/word/index")
                .badge(dueReviewCount > 0 ? "优先" : "今日主任务")
                .accent("#0f766e")
                .build());
        tasks.add(DashboardResponse.TaskCard.builder()
                .title(hasWeakModule(user, "阅读") ? "阅读定位训练 1 篇" : "阅读保持手感 1 篇")
                .subtitle("先找关键词，再回原文定位证据")
                .reason(hasWeakModule(user, "阅读") ? "你当前最需要提升的是定位与排错能力" : "保持真题触感，避免只背词不做题")
                .page("/pages/reading/index")
                .badge("思路训练")
                .accent("#b45309")
                .build());
        tasks.add(DashboardResponse.TaskCard.builder()
                .title("作文提纲输出 1 题")
                .subtitle("先口头审题，再说分论点和例子")
                .reason("先练结构和输出，再进入整篇写作，阻力更小")
                .page("/pages/writing/index")
                .badge("轻任务")
                .accent("#7c3aed")
                .build());
        return tasks;
    }

    private String mergeRecentTraining(String planJson, Map<String, Object> payload) throws Exception {
        Map<String, Object> root;
        if (planJson == null || planJson.isBlank()) {
            root = new HashMap<>();
        } else {
            root = objectMapper.readValue(planJson, Map.class);
        }
        root.put("_recentTraining", payload);
        return objectMapper.writeValueAsString(root);
    }

    private String stripRecentTraining(String planJson) throws Exception {
        Map<String, Object> root = objectMapper.readValue(planJson, Map.class);
        root.remove("_recentTraining");
        return objectMapper.writeValueAsString(root);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    private boolean hasWeakModule(User user, String keyword) {
        return user.getWeakModules() != null && user.getWeakModules().contains(keyword);
    }

    private String buildGreeting(User user) {
        if (user.getTargetScore() == null) {
            return "先完成学习画像，系统才能帮你安排更像样的训练路径。";
        }
        return "目标 " + user.getTargetScore() + " 分，今天先做最该做的 1 到 3 件事。";
    }

    private String buildRiskHint(User user) {
        String obstacle = defaultText(user.getBiggestObstacle(), "");
        if (obstacle.contains("坚持")) {
            return "今天不要贪多，先把第一项任务做完。";
        }
        if (obstacle.contains("时间")) {
            return "优先选 10 到 20 分钟能完成的训练块。";
        }
        if (obstacle.contains("作文")) {
            return "先练提纲和例子，不要直接逼自己写整篇。";
        }
        return "把任务拆小，优先完成最关键的一项。";
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }
}
