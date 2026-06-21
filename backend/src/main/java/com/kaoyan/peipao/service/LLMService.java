package com.kaoyan.peipao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaoyan.peipao.dto.response.PlanResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LLMService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url}")
    private String baseUrl;

    @Value("${deepseek.model}")
    private String model;

    @Value("${deepseek.timeout}")
    private int timeout;

    public LLMService(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
    }

    public PlanResponse generatePlan(Map<String, Object> userProfile) {
        String prompt = buildPrompt(userProfile);
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                String llmOutput = callDeepSeek(prompt);
                PlanResponse plan = parsePlanResponse(llmOutput);
                int totalTarget = plan.getScoreBreakdown().stream()
                        .mapToInt(PlanResponse.ScoreItem::getTargetScore).sum();
                int userTarget = (int) userProfile.get("targetScore");
                if (Math.abs(totalTarget - userTarget) <= 3) {
                    log.info("Plan OK attempt={}, total={}, target={}", attempt, totalTarget, userTarget);
                    return plan;
                }
                log.warn("Plan validation failed: total={}, target={}", totalTarget, userTarget);
            } catch (Exception e) {
                log.error("LLM attempt {} failed: {}", attempt, e.getMessage());
                if (attempt == 3) return buildFallbackPlan(userProfile);
            }
        }
        return buildFallbackPlan(userProfile);
    }

    private String buildPrompt(Map<String, Object> p) {
        String tpl = "你是考研英语辅导专家。请根据以下用户信息，生成一份专属学习方案。\n\n"
                + "【用户信息】\n"
                + "- 英语类型：%s\n"
                + "- 目标分数：%s 分\n"
                + "- 剩余天数：%s 天\n"
                + "- 每日学习时间：%s\n"
                + "- 周末是否学习：%s\n"
                + "- 当前英语水平：%s\n"
                + "- 已有资料：%s\n\n"
                + "【硬约束规则 - 必须严格遵守】\n"
                + "1. 六项目标分之和与用户目标分的偏差必须 <= 3 分\n"
                + "2. 基础弱（四级未过/低分飘过）：阅读26-28，大作文12-14，小作文6-7，新题型6-8，翻译4-5，完形3-4\n"
                + "3. 基础中等（四级高分/六级已过）：阅读28-32，大作文14-16，小作文7-8，新题型7-8，翻译5-6，完形4-5\n"
                + "4. 基础好（六级高分）：阅读30-34，大作文15-17，小作文7-9，新题型7-9，翻译6-7，完形5-6\n"
                + "5. 剩余天数<60：跳过语法阶段\n"
                + "6. 剩余天数>180：第一阶段35%%，第二阶段40%%，第三阶段25%%\n"
                + "7. 剩余天数60-180：第一阶段25%%，第二阶段45%%，第三阶段30%%\n\n"
                + "【输出格式 - 只输出 JSON】\n"
                + "{\n"
                + "  \"profile\": \"一句话用户画像\",\n"
                + "  \"scoreBreakdown\": [\n"
                + "    {\"type\":\"阅读理解\",\"fullScore\":40,\"targetScore\":数字,\"difficulty\":5},\n"
                + "    {\"type\":\"大作文\",\"fullScore\":20,\"targetScore\":数字,\"difficulty\":3},\n"
                + "    {\"type\":\"小作文\",\"fullScore\":10,\"targetScore\":数字,\"difficulty\":2},\n"
                + "    {\"type\":\"新题型\",\"fullScore\":10,\"targetScore\":数字,\"difficulty\":2},\n"
                + "    {\"type\":\"翻译\",\"fullScore\":10,\"targetScore\":数字,\"difficulty\":1},\n"
                + "    {\"type\":\"完形填空\",\"fullScore\":10,\"targetScore\":数字,\"difficulty\":0}\n"
                + "  ],\n"
                + "  \"phases\": [\n"
                + "    {\"name\":\"第一阶段：基础清零\",\"dayRange\":\"第1-X天\",\"focus\":\"词汇+语法长难句\"},\n"
                + "    {\"name\":\"第二阶段：阅读攻坚\",\"dayRange\":\"第X-Y天\",\"focus\":\"真题精读+错题归因\"},\n"
                + "    {\"name\":\"第三阶段：全题型+套卷\",\"dayRange\":\"第Y-Z天\",\"focus\":\"作文模板+新题型+模考\"}\n"
                + "  ],\n"
                + "  \"dailySchedule\": \"每日时间分配建议\",\n"
                + "  \"milestones\": [{\"day\":数字,\"description\":\"描述\"}]\n"
                + "}";
        return String.format(tpl,
                p.get("examType"), p.get("targetScore"), p.get("remainingDays"),
                p.get("dailyHours"), p.get("weekendAvailable"), p.get("englishLevel"),
                p.get("materials"));
    }

    private String callDeepSeek(String prompt) throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "你是一个考研英语辅导专家。只输出 JSON，不输出任何其他内容。"),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.0,
                "max_tokens", 2048
        ));
        var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofMillis(timeout))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("DeepSeek API error " + response.statusCode());
        }
        JsonNode root = objectMapper.readTree(response.body());
        return root.path("choices").get(0).path("message").path("content").asText();
    }

    private PlanResponse parsePlanResponse(String raw) throws Exception {
        String json = raw;
        if (json.contains("```json")) {
            json = json.substring(json.indexOf("```json") + 7);
            if (json.contains("```")) json = json.substring(0, json.indexOf("```"));
        } else if (json.contains("```")) {
            json = json.substring(json.indexOf("```") + 3);
            if (json.contains("```")) json = json.substring(0, json.indexOf("```"));
        }
        json = json.trim();
        JsonNode r = objectMapper.readTree(json);

        List<PlanResponse.ScoreItem> scores = new ArrayList<>();
        for (JsonNode it : r.path("scoreBreakdown")) {
            scores.add(PlanResponse.ScoreItem.builder()
                    .type(it.path("type").asText())
                    .fullScore(it.path("fullScore").asInt())
                    .targetScore(it.path("targetScore").asInt())
                    .difficulty(it.path("difficulty").asInt()).build());
        }

        List<PlanResponse.PhaseItem> phases = new ArrayList<>();
        for (JsonNode it : r.path("phases")) {
            phases.add(PlanResponse.PhaseItem.builder()
                    .name(it.path("name").asText())
                    .dayRange(it.path("dayRange").asText())
                    .focus(it.path("focus").asText()).build());
        }

        List<PlanResponse.MilestoneItem> milestones = new ArrayList<>();
        for (JsonNode it : r.path("milestones")) {
            milestones.add(PlanResponse.MilestoneItem.builder()
                    .day(it.path("day").asInt())
                    .description(it.path("description").asText()).build());
        }

        return PlanResponse.builder()
                .profile(r.path("profile").asText())
                .scoreBreakdown(scores).phases(phases)
                .dailySchedule(r.path("dailySchedule").asText())
                .milestones(milestones).build();
    }

    private PlanResponse buildFallbackPlan(Map<String, Object> p) {
        int t = (int) p.get("targetScore");
        int r = (int) Math.round(t * 0.44);
        int be = (int) Math.round(t * 0.22);
        int se = (int) Math.round(t * 0.11);
        int nt = (int) Math.round(t * 0.11);
        int tr = (int) Math.round(t * 0.07);
        int cl = t - r - be - se - nt - tr;
        int d = (int) p.get("remainingDays");
        int p1 = Math.max(1, (int) (d * 0.25));
        int p2 = p1 + Math.max(1, (int) (d * 0.45));
        return PlanResponse.builder()
                .profile("考研" + p.get("examType") + " " + p.get("englishLevel") + " 目标" + t + "分")
                .scoreBreakdown(List.of(
                        si("阅读理解",40,r,5), si("大作文",20,be,3), si("小作文",10,se,2),
                        si("新题型",10,nt,2), si("翻译",10,tr,1), si("完形填空",10,cl,0)))
                .phases(List.of(
                        ph("第一阶段：基础清零","第1-"+p1+"天","词汇+语法长难句"),
                        ph("第二阶段：阅读攻坚","第"+(p1+1)+"-"+p2+"天","真题精读+错题归因"),
                        ph("第三阶段：全题型+套卷","第"+(p2+1)+"-"+d+"天","作文模板+新题型+模考")))
                .dailySchedule("每天" + p.get("dailyHours") + "按阶段分配")
                .milestones(List.of(
                        mi((int)(d*0.2),"完成核心词汇第一轮"),
                        mi((int)(d*0.45),"精读真题20篇，阅读正确率突破60%"),
                        mi((int)(d*0.7),"第一次完整套卷模考")))
                .build();
    }

    private PlanResponse.ScoreItem si(String t, int f, int ts, int d) {
        return PlanResponse.ScoreItem.builder().type(t).fullScore(f).targetScore(ts).difficulty(d).build(); }
    private PlanResponse.PhaseItem ph(String n, String r, String f) {
        return PlanResponse.PhaseItem.builder().name(n).dayRange(r).focus(f).build(); }
    private PlanResponse.MilestoneItem mi(int d, String desc) {
        return PlanResponse.MilestoneItem.builder().day(d).description(desc).build(); }
}