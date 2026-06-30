package com.kaoyan.peipao.service;

import com.kaoyan.peipao.dto.request.PracticeCoachRequest;
import com.kaoyan.peipao.dto.response.ClozeTaskResponse;
import com.kaoyan.peipao.dto.response.PracticeCoachResponse;
import com.kaoyan.peipao.dto.response.TranslationTaskResponse;
import com.kaoyan.peipao.dto.response.WritingTaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeService {

    private final LLMService llmService;
    private final TokenActionGuardService tokenActionGuardService;
    private final PracticeArchiveService practiceArchiveService;

    public WritingTaskResponse getWritingTask(Long userId, String type) {
        String writingType = normalizeWritingType(type);
        log.info("[写作专项] 获取任务 userId={}, writingType={}", userId, writingType);
        return "large".equals(writingType) ? largeWritingTask() : smallWritingTask();
    }

    public PracticeCoachResponse coachWriting(PracticeCoachRequest request) {
        int turn = request.getTurn() == null ? 1 : request.getTurn();
        int step = request.getStep() == null ? 1 : request.getStep();
        String writingType = normalizeWritingType(request.getPracticeType());
        WritingTaskResponse task = "large".equals(writingType) ? largeWritingTask() : smallWritingTask();
        String selected = describeSelectedOptions(task, request.getSelectedOptionIds());
        String userAnswer = safe(request.getUserAnswer());
        log.info("[写作专项] 教练反馈 start userId={}, taskId={}, type={}, step={}, turn={}, selectedCount={}, answerChars={}",
                request.getUserId(), request.getTaskId(), writingType, step, turn,
                request.getSelectedOptionIds() == null ? 0 : request.getSelectedOptionIds().size(),
                userAnswer.length());
        tokenActionGuardService.guard(request.getUserId(), "writing-coach", Duration.ofSeconds(6));

        if (step >= 2) {
            return coachWritingFrame(request, task, writingType, selected);
        }

        String reply = llmService.generateWritingCoachReply(
                task.getPrompt(),
                writingType,
                selected,
                userAnswer,
                turn
        );
        boolean reveal = turn >= 2;
        practiceArchiveService.recordTraining(
                request.getUserId(),
                "writing",
                task.getTaskId(),
                task.getTitle(),
                task.getPrompt(),
                selected,
                userAnswer,
                reply,
                reveal
        );
        log.info("[写作专项] 教练反馈 done userId={}, taskId={}, reveal={}, replyChars={}",
                request.getUserId(), request.getTaskId(), reveal, reply.length());
        return PracticeCoachResponse.builder()
                .coachReply(reply)
                .nextQuestion("下面不用写整篇作文。先在高质量框架里补关键空白：可以写短语，也可以写 1-3 句。")
                .revealAnswer(false)
                .answer("")
                .explanation("先用框架托住结构和句式质量，再训练你在关键位置自由表达。")
                .writingFrame(writingFrame(writingType))
                .assetCandidates(List.of())
                .build();
    }

    private PracticeCoachResponse coachWritingFrame(PracticeCoachRequest request, WritingTaskResponse task, String writingType, String selected) {
        String filledText = formatFrameAnswers(request.getWritingFrameAnswers());
        String reply = llmService.generateWritingFrameReview(
                task.getPrompt(),
                writingType,
                selected,
                filledText
        );
        practiceArchiveService.recordTraining(
                request.getUserId(),
                "writing",
                task.getTaskId(),
                task.getTitle(),
                task.getPrompt(),
                selected,
                filledText,
                reply,
                true
        );
        log.info("[写作专项] 框架批改 done userId={}, taskId={}, answers={}, replyChars={}",
                request.getUserId(),
                task.getTaskId(),
                request.getWritingFrameAnswers() == null ? 0 : request.getWritingFrameAnswers().size(),
                reply.length());
        return PracticeCoachResponse.builder()
                .coachReply(reply)
                .nextQuestion("你可以把 AI 改写后的表达加入误解本，后续写作会反复用到。")
                .revealAnswer(true)
                .answer(writingSkeleton(writingType))
                .explanation("本次训练已完成：你没有写整篇空白作文，但已经完成了关键位置的自由表达和批改。")
                .writingFrame(writingFrame(writingType))
                .assetCandidates(writingAssets(writingType))
                .build();
    }

    public TranslationTaskResponse getTranslationTask(Long userId) {
        log.info("[翻译专项] 获取任务 userId={}", userId);
        return translationTask();
    }

    public PracticeCoachResponse coachTranslation(PracticeCoachRequest request) {
        TranslationTaskResponse task = translationTask();
        int step = request.getStep() == null ? 1 : request.getStep();
        log.info("[翻译专项] 教练反馈 start userId={}, taskId={}, step={}, mainChars={}, modifierChars={}, translationChars={}",
                request.getUserId(), request.getTaskId(), step,
                safe(request.getMainStructure()).length(),
                safe(request.getModifiers()).length(),
                safe(request.getTranslation()).length());
        tokenActionGuardService.guard(request.getUserId(), "translation-coach", Duration.ofSeconds(6));
        String reply = llmService.generateTranslationCoachReply(
                task.getSentence(),
                safe(request.getMainStructure()),
                safe(request.getModifiers()),
                safe(request.getTranslation()),
                step
        );
        boolean reveal = step >= 3;
        practiceArchiveService.recordTraining(
                request.getUserId(),
                "translation",
                task.getTaskId(),
                task.getTitle(),
                task.getSentence(),
                "step-" + step,
                safe(request.getTranslation()).isBlank() ? safe(request.getMainStructure()) : safe(request.getTranslation()),
                reply,
                reveal
        );
        log.info("[翻译专项] 教练反馈 done userId={}, taskId={}, reveal={}, replyChars={}",
                request.getUserId(), request.getTaskId(), reveal, reply.length());
        return PracticeCoachResponse.builder()
                .coachReply(reply)
                .nextQuestion(reveal ? "下一句继续按“主干-修饰-整句”三步走。" : "继续补充下一步，不要急着看标准译文。")
                .revealAnswer(reveal)
                .answer(reveal ? task.getStandardTranslation() : "")
                .explanation(reveal ? "本句重点是让步状语从句和主句因果关系，中文要先顺逻辑，再顺表达。" : "")
                .assetCandidates(translationAssets())
                .build();
    }

    public ClozeTaskResponse getClozeTask(Long userId) {
        log.info("[完形专项] 获取任务 userId={}", userId);
        return clozeTask();
    }

    public PracticeCoachResponse coachCloze(PracticeCoachRequest request) {
        ClozeTaskResponse task = clozeTask();
        int turn = request.getTurn() == null ? 1 : request.getTurn();
        String selected = safe(request.getSelectedOption()).toUpperCase(Locale.ROOT);
        String reasoning = safe(request.getUserAnswer());
        log.info("[完形专项] 教练反馈 start userId={}, taskId={}, selected={}, turn={}, reasoningChars={}",
                request.getUserId(), request.getTaskId(), selected, turn, reasoning.length());
        tokenActionGuardService.guard(request.getUserId(), "cloze-coach", Duration.ofSeconds(6));
        String reply = llmService.generateClozeCoachReply(
                task.getPassage(),
                task.getStem(),
                selected,
                reasoning,
                turn
        );
        boolean reveal = turn >= 2;
        practiceArchiveService.recordTraining(
                request.getUserId(),
                "cloze",
                task.getTaskId(),
                task.getTitle(),
                task.getPassage(),
                selected,
                reasoning,
                reply,
                reveal
        );
        log.info("[完形专项] 教练反馈 done userId={}, taskId={}, reveal={}, replyChars={}",
                request.getUserId(), request.getTaskId(), reveal, reply.length());
        return PracticeCoachResponse.builder()
                .coachReply(reply)
                .nextQuestion(reveal ? "下一空继续先说依据，再看答案。" : "先别急着改答案，说清楚你排除另外三个选项的理由。")
                .revealAnswer(reveal)
                .answer(reveal ? "B. contribute to" : "")
                .explanation(reveal ? "空格后接的是 broader public understanding，语义是“有助于形成更广泛的公众理解”，contribute to 同时满足搭配和语义。" : "")
                .assetCandidates(clozeAssets())
                .build();
    }

    private WritingTaskResponse smallWritingTask() {
        return WritingTaskResponse.builder()
                .taskId("writing-small-001")
                .writingType("small")
                .title("小作文：建议信")
                .goal("先解决不会开头、不会展开、不会结尾。")
                .prompt("你的同学准备考研英语，但一直不知道如何安排阅读训练。请写一封建议信，说明阅读训练的重要性，并给出两条具体建议。")
                .thinkingQuestions(List.of(
                        question("q1", "开头应该怎么起笔？",
                                option("q1-a", "A", "先说明写信目的：我写信是想给你一些阅读训练建议"),
                                option("q1-b", "B", "先回应对方困惑：我理解你现在不知道从哪里练阅读"),
                                option("q1-c", "C", "先表达积极态度：阅读能力完全可以通过方法训练提升"),
                                option("q1-d", "D", "先直接给结论：你最需要的是定位训练和错因复盘"),
                                option("q1-e", "E", "先铺垫背景：考研阅读不仅考词汇，也考定位和判断")),
                        question("q2", "中间逻辑应该如何展开？",
                                option("q2-a", "A", "按“问题 -> 方法 -> 好处”展开：没方向，所以先练定位，再复盘错因"),
                                option("q2-b", "B", "按“两条建议并列”展开：第一做定位训练，第二建立错因记录"),
                                option("q2-c", "C", "按“先易后难”展开：先解决题干定位，再处理长难句和选项排除"),
                                option("q2-d", "D", "按“原因 -> 措施”展开：阅读错不是只因词汇，而是缺少判断路径"),
                                option("q2-e", "E", "按“具体执行计划”展开：每天一篇精读，每周复盘错因")),
                        question("q3", "结尾应该怎么收束？",
                                option("q3-a", "A", "礼貌鼓励：希望这些建议能帮你更稳定地训练阅读"),
                                option("q3-b", "B", "表达支持：如果你需要，我也愿意继续和你讨论计划"),
                                option("q3-c", "C", "再次强调行动：关键是坚持定位、复盘和调整"),
                                option("q3-d", "D", "保持简洁礼貌：期待看到你的进步"),
                                option("q3-e", "E", "回扣目的：愿这些方法让你的阅读训练更有方向"))
                ))
                .build();
    }

    private WritingTaskResponse largeWritingTask() {
        return WritingTaskResponse.builder()
                .taskId("writing-large-001")
                .writingType("large")
                .title("大作文：时间管理")
                .goal("先搭建图画/图表作文的描述、解释、总结三段骨架。")
                .prompt("某幅图画中，一个学生被手机消息、会议提醒和待办事项包围，却很难真正完成一件事。请围绕注意力与深度学习写一篇短文。")
                .thinkingQuestions(List.of(
                        question("q1", "开头应该怎么起笔？",
                                option("q1-a", "A", "先客观描述画面：学生被消息、提醒和待办包围"),
                                option("q1-b", "B", "先点明现象：信息便利正在切碎人的注意力"),
                                option("q1-c", "C", "先提出中心观点：效率工具不等于真正的深度学习"),
                                option("q1-d", "D", "先从对比切入：看似忙碌，实则缺少有效进展"),
                                option("q1-e", "E", "先引出社会背景：数字工具正在改变学习方式")),
                        question("q2", "中间逻辑应该如何展开？",
                                option("q2-a", "A", "按“现象 -> 原因 -> 影响”展开：干扰多，所以专注变浅，理解变弱"),
                                option("q2-b", "B", "按“两点原因”展开：即时反馈诱惑强，学习任务缺少边界"),
                                option("q2-c", "C", "按“利弊平衡”展开：工具能提效，但过度依赖会削弱耐心"),
                                option("q2-d", "D", "按“个人 -> 社会”展开：个人学习效率下降，长期影响思考能力"),
                                option("q2-e", "E", "按“问题 -> 对策”展开：减少干扰、设置专注时段、延迟回复")),
                        question("q3", "结尾应该怎么收束？",
                                option("q3-a", "A", "提出建议：在效率工具和深度专注之间取得平衡"),
                                option("q3-b", "B", "升华观点：真正的学习需要持续注意力和耐心"),
                                option("q3-c", "C", "回扣画面：不要让忙碌的提醒替代真实进步"),
                                option("q3-d", "D", "给出行动方向：主动管理信息，而不是被信息管理"),
                                option("q3-e", "E", "总结态度：技术应服务学习，而不应支配学习"))
                ))
                .build();
    }

    private TranslationTaskResponse translationTask() {
        return TranslationTaskResponse.builder()
                .taskId("translation-001")
                .title("长难句拆解：让步 + 因果")
                .sentence("Although instant access to information appears to make learning more efficient, it often prevents students from developing the patience required for deep understanding.")
                .hint("先找主句，再处理 although 引导的让步部分。")
                .standardTranslation("虽然即时获取信息看似让学习更高效，但它往往会阻碍学生培养深入理解所需要的耐心。")
                .checkpoints(List.of("主干：it prevents students from developing patience", "让步：Although instant access...more efficient", "后置修饰：required for deep understanding"))
                .build();
    }

    private ClozeTaskResponse clozeTask() {
        return ClozeTaskResponse.builder()
                .taskId("cloze-001")
                .title("完形单空：搭配 + 语义")
                .passage("Research does not become valuable simply because it is published. It matters when its findings can _____ broader public understanding and help people make better decisions.")
                .stem("空格处最适合填入哪一项？")
                .options(List.of(
                        ClozeTaskResponse.OptionItem.builder().label("A").content("depend on").build(),
                        ClozeTaskResponse.OptionItem.builder().label("B").content("contribute to").build(),
                        ClozeTaskResponse.OptionItem.builder().label("C").content("turn down").build(),
                        ClozeTaskResponse.OptionItem.builder().label("D").content("break into").build()
                ))
                .build();
    }

    private WritingTaskResponse.ThinkingQuestion question(String id, String title, WritingTaskResponse.OptionItem... options) {
        return WritingTaskResponse.ThinkingQuestion.builder()
                .id(id)
                .title(title)
                .options(List.of(options))
                .build();
    }

    private WritingTaskResponse.OptionItem option(String id, String label, String content) {
        return WritingTaskResponse.OptionItem.builder().id(id).label(label).content(content).build();
    }

    private String describeSelectedOptions(WritingTaskResponse task, List<String> selectedIds) {
        if (selectedIds == null || selectedIds.isEmpty()) {
            return "用户还没有选择结构方向。";
        }
        StringJoiner joiner = new StringJoiner("；");
        for (WritingTaskResponse.ThinkingQuestion q : task.getThinkingQuestions()) {
            for (WritingTaskResponse.OptionItem option : q.getOptions()) {
                if (selectedIds.contains(option.getId())) {
                    joiner.add(q.getTitle() + " -> " + option.getContent());
                }
            }
        }
        return joiner.length() == 0 ? "用户选择内容无法匹配。" : joiner.toString();
    }

    private String writingSkeleton(String writingType) {
        if ("large".equals(writingType)) {
            return "起笔：描述画面/图表并点明中心现象。展开：围绕原因、影响、利弊或对策组织逻辑，至少展开两点。收束：回扣主题，给出观点或行动建议。";
        }
        return "起笔：说明写信目的并回应对方处境。展开：给出具体、可执行的建议或说明，避免空话。收束：礼貌表达希望建议有帮助，并保持应用文语气。";
    }

    private PracticeCoachResponse.WritingFrame writingFrame(String writingType) {
        if ("large".equals(writingType)) {
            return PracticeCoachResponse.WritingFrame.builder()
                    .title("大作文高质量框架：局部自由写作")
                    .instruction("不用写整篇。每个空都可以写短语、一个句子，或 1-3 句；系统负责结构和句式，你负责填入真实观点。")
                    .lines(List.of(
                            frameLine("l1", "The picture vividly reveals a common problem in modern learning: ", ".", "点明现象", "写出图画反映的问题，例如注意力被切碎、看似忙碌但缺少深度进展", 500),
                            frameLine("l2", "This phenomenon is not accidental. One major reason is that ", ".", "原因一", "写一个原因，可以用中文想好再写英文", 500),
                            frameLine("l3", "Another factor worth noting is that ", ".", "原因二或影响", "补充另一个原因、影响或生活例子", 500),
                            frameLine("l4", "Therefore, it is necessary for students to ", ".", "行动建议", "写一个具体建议，例如设置专注时段、延迟回复消息", 500),
                            frameLine("l5", "Only in this way can they ", ".", "结果收束", "写出这样做带来的结果，例如真正理解知识、形成深度思考", 500)
                    ))
                    .build();
        }
        return PracticeCoachResponse.WritingFrame.builder()
                .title("小作文高质量框架：局部自由写作")
                .instruction("不用写完整信件。每个空都可以写短语、一个句子，或 1-3 句；重点是把建议写具体。")
                .lines(List.of(
                        frameLine("s1", "I am writing to offer some practical suggestions on ", ".", "写信目的", "写你要建议的主题，例如 how to improve reading performance", 400),
                        frameLine("s2", "To begin with, it would be advisable for you to ", ".", "第一条建议", "写一个具体动作，例如 practice locating key information before checking options", 500),
                        frameLine("s3", "This is because ", ".", "建议理由", "解释为什么这条建议有效，可以写 1-2 句", 500),
                        frameLine("s4", "In addition, you may ", " so that you can make your reading practice more systematic.", "第二条建议", "写第二个具体动作，例如 keep a record of mistakes and review them weekly", 500),
                        frameLine("s5", "I hope these suggestions will ", ".", "礼貌收束", "写希望对方获得什么帮助，例如 help you build a clearer training routine", 400)
                ))
                .build();
    }

    private PracticeCoachResponse.FrameLine frameLine(String id, String beforeText, String afterText, String label, String placeholder, int maxLength) {
        return PracticeCoachResponse.FrameLine.builder()
                .id(id)
                .beforeText(beforeText)
                .afterText(afterText)
                .label(label)
                .placeholder(placeholder)
                .maxLength(maxLength)
                .build();
    }

    private String formatFrameAnswers(List<PracticeCoachRequest.WritingFrameAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            return "用户还没有填写框架空白。";
        }
        StringJoiner joiner = new StringJoiner("\n");
        for (PracticeCoachRequest.WritingFrameAnswer answer : answers) {
            joiner.add("%s（%s）：%s".formatted(
                    safe(answer.getLabel()),
                    safe(answer.getPrompt()),
                    safe(answer.getAnswer()).isBlank() ? "未填写" : safe(answer.getAnswer())
            ));
        }
        return joiner.toString();
    }

    private List<PracticeCoachResponse.AssetCandidate> writingAssets(String writingType) {
        if ("large".equals(writingType)) {
            return List.of(
                    asset("sentence", "strike a balance between efficiency and concentration", "在效率和专注之间取得平衡", "写作表达"),
                    asset("sentence", "develop the patience required for deep understanding", "培养深入理解所需要的耐心", "固定搭配")
            );
        }
        return List.of(
                asset("sentence", "I am writing to offer some practical suggestions.", "我写信是想提供一些实用建议。", "写作表达"),
                asset("sentence", "It would be advisable to review your mistakes regularly.", "定期复盘错误会是明智的做法。", "固定搭配")
        );
    }

    private List<PracticeCoachResponse.AssetCandidate> translationAssets() {
        return List.of(
                asset("sentence", "instant access to information", "即时获取信息", "固定搭配"),
                asset("sentence", "prevent sb. from doing sth.", "阻止某人做某事", "固定搭配")
        );
    }

    private List<PracticeCoachResponse.AssetCandidate> clozeAssets() {
        return List.of(
                asset("sentence", "contribute to", "有助于；促成", "固定搭配"),
                asset("word", "depend on", "依赖；取决于", "易混词")
        );
    }

    private PracticeCoachResponse.AssetCandidate asset(String type, String sourceText, String translation, String sourceModule) {
        return PracticeCoachResponse.AssetCandidate.builder()
                .type(type)
                .sourceText(sourceText)
                .translation(translation)
                .sourceModule(sourceModule)
                .build();
    }

    private String normalizeWritingType(String type) {
        return "large".equalsIgnoreCase(type) ? "large" : "small";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
