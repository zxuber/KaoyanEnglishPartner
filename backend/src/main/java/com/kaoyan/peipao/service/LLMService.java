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
                    log.info("[大模型] 方案生成成功 attempt={}, total={}, target={}", attempt, totalTarget, userTarget);
                    return plan;
                }
                log.warn("[大模型] 方案校验失败 total={}, target={}", totalTarget, userTarget);
            } catch (Exception e) {
                log.error("[大模型] 第{}次调用失败: {}", attempt, e.getMessage());
                if (attempt == 3) return buildFallbackPlan(userProfile);
            }
        }
        return buildFallbackPlan(userProfile);
    }

    public String generateReadingCoachReply(
            String title,
            String passage,
            String stem,
            String focus,
            String standardAnswer,
            String explanation,
            String userAnswer,
            String selectedOption,
            int turn
    ) {
        String prompt = """
                你是一个考研英语阅读一对一教练。你的任务不是直接公布答案，而是先根据用户的回答继续追问和引导。

                【文章标题】
                %s

                【文章内容】
                %s

                【题目】
                %s

                【考查点】
                %s

                【标准答案】
                %s

                【答案解析】
                %s

                【用户当前回答】
                %s

                【用户当前倾向选项】
                %s

                【当前轮次】
                第 %d 轮

                输出要求：
                1. 如果是第 1 轮或第 2 轮，优先指出用户思路中的关键缺口，并只给一句追问式引导。
                2. 不要直接说“正确答案是……”。
                3. 语气像真人老师，简洁、明确、能继续问下去。
                4. 只输出纯文本，不要输出 JSON，不要编号。
                """.formatted(title, passage, stem, focus, standardAnswer, explanation, userAnswer, selectedOption == null ? "未选择" : selectedOption, turn);

        try {
            return callDeepSeekText(prompt, "你是一个考研英语阅读教练。输出简短中文引导，不要输出 JSON。");
        } catch (Exception e) {
            log.warn("[大模型] 阅读教练降级: {}", e.getMessage());
            return switch (turn) {
                case 1 -> "先别急着选答案，你先说说题干问的是主旨、细节还是作者态度？对应信息大概落在哪一段？";
                case 2 -> "继续往前推一步：把你锁定的那一句原文复述出来，再说它为什么能支撑你的判断。";
                default -> "你已经接近答案了，回到题干关键词，再核对原文里最直接对应的句子。";
            };
        }
    }

    public String translateSelection(String sourceText, String contentType) {
        String prompt = """
                你是一个考研英语助手。请把下面的英文翻译成自然、简洁、适合考研复盘使用的中文。

                【内容类型】
                %s

                【英文内容】
                %s

                要求：
                1. 如果是单词，优先给出 1-2 个最常用中文义项。
                2. 如果是短句，直接给出自然中文译文。
                3. 不要编号，不要额外解释，不要输出原文。
                """.formatted(contentType, sourceText);

        try {
            return callDeepSeekText(prompt, "你是一个精简的英汉翻译助手，只输出中文结果。");
        } catch (Exception e) {
            log.warn("[大模型] 翻译降级: {}", e.getMessage());
            return "暂未获取翻译，请稍后重试";
        }
    }

    public String generateWritingCoachReply(
            String promptText,
            String writingType,
            String selectedStructure,
            String userAnswer,
            int turn
    ) {
        String prompt = """
                你是一个考研英语写作教练。这个产品的目标是解决用户“不会开头、不会展开、不会结尾”。

                【作文类型】
                %s

                【题目】
                %s

                【用户选择的结构方向】
                %s

                【用户用大白话补充的想法】
                %s

                【当前轮次】
                第 %d 轮

                输出要求：
                1. 先分析用户选择的开头起笔、中间展开、结尾收束方向是否合理，明确指出有无偏差。
                2. 再判断用户补充内容属于“真实作文思路”还是“求助/焦虑/发牢骚”。
                3. 如果用户是在求助，比如不会起笔、不会分段、词汇量不足，先鼓励并降低任务难度，告诉他下一步只需要在框架空白处写短语或1-3句。
                4. 如果用户确实在围绕题目表达思路，则分析是否贴题、是否具体、逻辑是否顺。
                5. 不要写完整作文，不要输出 JSON，不要继续假装用户已经完成了作文思路。
                """.formatted(writingType, promptText, selectedStructure, userAnswer, turn);

        try {
            return callDeepSeekText(prompt, "你是一个考研英语写作教练。用中文输出简洁、具体、可执行的纠偏反馈。");
        } catch (Exception e) {
            log.warn("[大模型] 写作教练降级: {}", e.getMessage());
            return "你的方向可以先保留。你现在暴露出的主要问题不是审题错误，而是写作启动困难：不知道如何起笔、如何分段、如何用更合适的表达。这个很正常，下一步先不用写整篇，只在框架空白里写短语或1-3句，我会再帮你改成更考试化的表达。";
        }
    }

    public String generateWritingFrameReview(
            String promptText,
            String writingType,
            String selectedStructure,
            String filledText
    ) {
        String prompt = """
                你是一个考研英语写作批改教练。用户没有写完整作文，而是在高质量半填空框架里完成了局部自由写作。

                【作文类型】
                %s

                【题目】
                %s

                【用户此前选择的结构方向】
                %s

                【用户填写的框架空白】
                %s

                输出要求：
                1. 先整体判断用户填写内容是否贴题，是否能支撑这篇作文。
                2. 逐条指出最明显的问题：语法、表达中式、逻辑空泛、内容不具体，最多 3 条。
                3. 给出对应的考试化改写。改写要自然、有考研作文质感，但不要堆砌生僻词。
                4. 提取 2 个值得沉淀的短语或句型，并说明可用于什么场景。
                5. 用中文反馈；英文改写可以直接写英文句子；不要输出 JSON。
                """.formatted(writingType, promptText, selectedStructure, filledText);

        try {
            return callDeepSeekText(prompt, "你是一个严格但鼓励型的考研英语写作批改教练。重点批改用户填入框架的局部表达。");
        } catch (Exception e) {
            log.warn("[大模型] 写作框架批改降级: {}", e.getMessage());
            return "这次你已经完成了关键位置的表达训练。先检查三点：是否贴题、是否具体、是否有明确动作。建议把空泛表达改成更具体的动作，比如把“read more”升级为“practice locating key information before checking the options”。";
        }
    }

    public String generateTranslationCoachReply(
            String sentence,
            String mainStructure,
            String modifiers,
            String translation,
            int step
    ) {
        String prompt = """
                你是一个考研英语翻译教练。请按“找主干 -> 拆修饰 -> 顺中文”的训练法纠偏。

                【原句】
                %s

                【用户主干判断】
                %s

                【用户修饰成分判断】
                %s

                【用户整句翻译】
                %s

                【当前步骤】
                第 %d 步

                输出要求：
                1. 不要一上来给标准译文。
                2. 先指出主干或修饰成分里最关键的一个问题。
                3. 给用户下一步应该怎么改。
                4. 如果已经到第 3 步，可以评价译文是否顺中文。
                5. 只输出中文纯文本。
                """.formatted(sentence, mainStructure, modifiers, translation, step);

        try {
            return callDeepSeekText(prompt, "你是一个考研英语翻译教练。用中文输出短反馈和下一步动作。");
        } catch (Exception e) {
            log.warn("[大模型] 翻译教练降级: {}", e.getMessage());
            return "先抓主句：it often prevents students from developing the patience。不要先翻 although 部分，主干稳了再处理让步信息。";
        }
    }

    public String generateClozeCoachReply(
            String passage,
            String stem,
            String selectedOption,
            String reasoning,
            int turn
    ) {
        String prompt = """
                你是一个考研英语完形填空教练。你的任务是训练用户说出选择依据，而不是只判断对错。

                【上下文】
                %s

                【题干】
                %s

                【用户选择】
                %s

                【用户理由】
                %s

                【当前轮次】
                第 %d 轮

                输出要求：
                1. 先判断用户理由属于语义、搭配、逻辑还是猜测。
                2. 指出最关键的上下文线索。
                3. 不要在第 1 轮直接公布正确答案。
                4. 只追问一个能帮助排除干扰项的问题。
                5. 只输出中文纯文本。
                """.formatted(passage, stem, selectedOption, reasoning, turn);

        try {
            return callDeepSeekText(prompt, "你是一个考研英语完形教练。用中文输出简短纠偏和追问。");
        } catch (Exception e) {
            log.warn("[大模型] 完形教练降级: {}", e.getMessage());
            return "先看空格后的 broader public understanding，这里需要一个能接抽象名词、表示“有助于”的搭配。你再说说为什么其他三个不合适？";
        }
    }

    private String buildPrompt(Map<String, Object> p) {
        String tpl = "你是考研英语辅导专家。请根据以下用户信息，生成一份专属学习方案。\n\n"
                + "【用户信息】\n"
                + "- 英语类型：%s\n"
                + "- 目标分数：%s 分\n"
                + "- 剩余天数：%s 天\n"
                + "- 最近一次真题/模拟分数区间：%s\n"
                + "- 当前英语水平：%s\n"
                + "- 薄弱模块：%s\n"
                + "- 工作日学习时间：%s\n"
                + "- 周末学习时间：%s\n"
                + "- 最稳定学习时段：%s\n"
                + "- 计划风格偏好：%s\n"
                + "- 当前最大卡点：%s\n"
                + "- 已有资料：%s\n\n"
                + "【硬约束规则 - 必须严格遵守】\n"
                + "1. 六项目标分之和与用户目标分的偏差必须 <= 3 分\n"
                + "2. 基础弱（四级未过/低分飘过）：阅读26-28，大作文12-14，小作文6-7，新题型6-8，翻译4-5，完形3-4\n"
                + "3. 基础中等（四级高分/六级已过）：阅读28-32，大作文14-16，小作文7-8，新题型7-8，翻译5-6，完形4-5\n"
                + "4. 基础好（六级高分）：阅读30-34，大作文15-17，小作文7-9，新题型7-9，翻译6-7，完形5-6\n"
                + "5. 如果薄弱模块包含阅读/词汇，第一阶段和第二阶段必须体现更高优先级\n"
                + "6. 如果剩余天数<60：压缩基础阶段，直接进入提分导向\n"
                + "7. 输出必须具体，可执行，避免空话\n\n"
                + "【输出格式 - 只输出 JSON】\n"
                + "{\n"
                + "  \"profile\": \"一句话用户画像\",\n"
                + "  \"diagnosisSummary\": [\"3条以内的关键诊断\"],\n"
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
                + "  \"weeklyFocus\": [\n"
                + "    {\"label\":\"前两周重点\",\"tasks\":\"任务描述\"},\n"
                + "    {\"label\":\"中期重点\",\"tasks\":\"任务描述\"}\n"
                + "  ],\n"
                + "  \"weekdayTemplate\": \"工作日学习模板\",\n"
                + "  \"weekendTemplate\": \"周末学习模板\",\n"
                + "  \"riskTip\": \"当前最容易掉队的风险\",\n"
                + "  \"catchUpAdvice\": \"如果中断3-7天如何恢复\",\n"
                + "  \"milestones\": [{\"day\":数字,\"description\":\"描述\"}]\n"
                + "}";
        return String.format(tpl,
                p.get("examType"), p.get("targetScore"), p.get("remainingDays"),
                p.get("currentScoreBand"), p.get("englishLevel"), p.get("weakModules"),
                p.get("weekdayHours"), p.get("weekendHours"), p.get("studyTimeSlot"),
                p.get("planStyle"), p.get("biggestObstacle"), p.get("materials"));
    }

    private String callDeepSeek(String prompt) throws Exception {
        return callDeepSeekText(prompt, "你是一个考研英语辅导专家。只输出 JSON，不输出任何其他内容。");
    }

    private String callDeepSeekText(String prompt, String systemPrompt) throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
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

        List<String> diagnosisSummary = new ArrayList<>();
        for (JsonNode it : r.path("diagnosisSummary")) {
            diagnosisSummary.add(it.asText());
        }

        List<PlanResponse.WeekFocusItem> weeklyFocus = new ArrayList<>();
        for (JsonNode it : r.path("weeklyFocus")) {
            weeklyFocus.add(PlanResponse.WeekFocusItem.builder()
                    .label(it.path("label").asText())
                    .tasks(it.path("tasks").asText()).build());
        }

        return PlanResponse.builder()
                .profile(r.path("profile").asText())
                .diagnosisSummary(diagnosisSummary)
                .scoreBreakdown(scores).phases(phases)
                .weeklyFocus(weeklyFocus)
                .weekdayTemplate(r.path("weekdayTemplate").asText())
                .weekendTemplate(r.path("weekendTemplate").asText())
                .riskTip(r.path("riskTip").asText())
                .catchUpAdvice(r.path("catchUpAdvice").asText())
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
                .profile("考研" + p.get("examType") + " " + p.get("englishLevel") + "，目标" + t + "分")
                .diagnosisSummary(List.of(
                        "当前基础为" + p.get("englishLevel") + "，目标分数需要分阶段推进",
                        "薄弱项优先处理：" + p.get("weakModules"),
                        "当前最大风险是：" + p.get("biggestObstacle")))
                .scoreBreakdown(List.of(
                        si("阅读理解",40,r,5), si("大作文",20,be,3), si("小作文",10,se,2),
                        si("新题型",10,nt,2), si("翻译",10,tr,1), si("完形填空",10,cl,0)))
                .phases(List.of(
                        ph("第一阶段：基础清零","第1-"+p1+"天","词汇+语法长难句"),
                        ph("第二阶段：阅读攻坚","第"+(p1+1)+"-"+p2+"天","真题精读+错题归因"),
                        ph("第三阶段：全题型+套卷","第"+(p2+1)+"-"+d+"天","作文模板+新题型+模考")))
                .weeklyFocus(List.of(
                        wf("前两周重点", "先补词汇和最薄弱模块，建立稳定节奏"),
                        wf("中期重点", "阅读真题精读 + 作文模板积累")))
                .weekdayTemplate("工作日" + p.get("weekdayHours") + "：前半段处理核心任务，后半段做复盘与记忆强化")
                .weekendTemplate("周末" + p.get("weekendHours") + "：安排完整真题训练或阶段复盘")
                .riskTip("最容易掉队的点是" + p.get("biggestObstacle") + "，需要把任务拆小并保持固定节奏")
                .catchUpAdvice("如果中断 3-7 天，先恢复词汇和阅读主线，不要试图一次补完全部任务")
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
    private PlanResponse.WeekFocusItem wf(String l, String t) {
        return PlanResponse.WeekFocusItem.builder().label(l).tasks(t).build(); }
}
