package com.kaoyan.peipao.service;

import com.kaoyan.peipao.dto.request.OnboardingRequest;
import com.kaoyan.peipao.dto.response.PlanResponse;
import com.kaoyan.peipao.entity.User;
import com.kaoyan.peipao.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final LLMService llmService;
    private final ObjectMapper objectMapper;

    public Map<String, Object> submitOnboarding(OnboardingRequest request) {
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

        log.info("Generating plan: target={}, days={}", request.getTargetScore(), request.getRemainingDays());
        PlanResponse plan = llmService.generatePlan(profile);

        User user = new User();
        user.setSessionId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
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
        user.setCurrentPhase("phase1");
        user.setPhaseStartDay(1);
        userMapper.insert(user);
        log.info("User created: id={}, sessionId={}", user.getId(), user.getSessionId());

        Map<String, Object> result = new HashMap<>();
        result.put("plan", plan);
        result.put("userId", user.getId());
        return result;
    }

    public PlanResponse getPlan(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getPlanJson() == null) {
            throw new RuntimeException("方案不存在");
        }
        try {
            return objectMapper.readValue(user.getPlanJson(), PlanResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("方案数据解析失败", e);
        }
    }
}
