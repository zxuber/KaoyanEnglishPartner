package com.kaoyan.peipao.controller;

import com.kaoyan.peipao.common.Result;
import java.util.Map;
import com.kaoyan.peipao.dto.request.OnboardingRequest;
import com.kaoyan.peipao.dto.request.UpdateRecentTrainingRequest;
import com.kaoyan.peipao.dto.response.DashboardResponse;
import com.kaoyan.peipao.dto.response.PlanResponse;
import com.kaoyan.peipao.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * M1: 提交学习画像，生成专属学习方案
     * POST /api/v1/users/onboarding
     */
    @PostMapping("/onboarding")
    public Result<Map<String, Object>> onboarding(@Valid @RequestBody OnboardingRequest request) {
        Map<String, Object> result = userService.submitOnboarding(request);
        return Result.ok(result);
    }

    @GetMapping("/{id}/dashboard")
    public Result<DashboardResponse> getDashboard(@PathVariable Long id) {
        return Result.ok(userService.getDashboard(id));
    }

    @PostMapping("/{id}/checkin")
    public Result<Void> checkin(@PathVariable Long id) {
        userService.checkin(id);
        return Result.ok(null);
    }

    @PostMapping("/{id}/recent-training")
    public Result<Void> updateRecentTraining(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRecentTrainingRequest request
    ) {
        userService.updateRecentTraining(id, request);
        return Result.ok(null);
    }

    /**
     * M1: 获取用户的专属方案
     * GET /api/v1/users/{id}/plan
     */
    @GetMapping("/{id}/plan")
    public Result<PlanResponse> getPlan(@PathVariable Long id) {
        PlanResponse plan = userService.getPlan(id);
        return Result.ok(plan);
    }
}
