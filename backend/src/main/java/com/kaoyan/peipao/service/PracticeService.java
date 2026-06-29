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

    public WritingTaskResponse getWritingTask(Long userId, String type) {
        String writingType = normalizeWritingType(type);
        log.info("[写作专项] 获取任务 userId={}, writingType={}", userId, writingType);
        return "large".equals(writingType) ? largeWritingTask() : smallWritingTask();
    }

    public PracticeCoachResponse coachWriting(PracticeCoachRequest request) {
        int turn = request.getTurn() == null ? 1 : request.getTurn();
        String writingType = normalizeWritingType(request.getPracticeType());
        WritingTaskResponse task = "large".equals(writingType) ? largeWritingTask() : smallWritingTask();
        String selected = describeSelectedOptions(task, request.getSelectedOptionIds());
        String userAnswer = safe(request.getUserAnswer());
        log.info("[写作专项] 教练反馈 start userId={}, taskId={}, type={}, turn={}, selectedCount={}, answerChars={}",
                request.getUserId(), request.getTaskId(), writingType, turn,
                request.getSelectedOptionIds() == null ? 0 : request.getSelectedOptionIds().size(),
                userAnswer.length());
        tokenActionGuardService.guard(request.getUserId(), "writing-coach", Duration.ofSeconds(6));

        String reply = llmService.generateWritingCoachReply(
                task.getPrompt(),
                writingType,
                selected,
                userAnswer,
                turn
        );
        boolean reveal = turn >= 2;
        log.info("[写作专项] 教练反馈 done userId={}, taskId={}, reveal={}, replyChars={}",
                request.getUserId(), request.getTaskId(), reveal, reply.length());
        return PracticeCoachResponse.builder()
                .coachReply(reply)
                .nextQuestion(reveal ? "把这套结构迁移到下一道题，重点检查开头目的和主体展开是否够具体。" : "继续用大白话补一句：你第二段最想说服对方接受什么？")
                .revealAnswer(reveal)
                .answer(reveal ? writingSkeleton(writingType) : "")
                .explanation(reveal ? "写作 MVP 先不追求整篇成文，先把审题、结构、展开和可复用表达跑通。" : "")
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
                        question("q1", "第一段应该先完成什么？",
                                option("q1-a", "A", "直接开始讲两条建议"),
                                option("q1-b", "B", "说明写信目的，并点出对方当前困惑"),
                                option("q1-c", "C", "先道歉，再解释原因")),
                        question("q2", "第二段最应该展开哪两类内容？",
                                option("q2-a", "A", "阅读定位训练 + 错因复盘"),
                                option("q2-b", "B", "背模板 + 多听听力"),
                                option("q2-c", "C", "只强调坚持，不给具体方法")),
                        question("q3", "结尾应该保留什么语气？",
                                option("q3-a", "A", "礼貌鼓励，并表达希望建议有帮助"),
                                option("q3-b", "B", "强硬要求对方照做"),
                                option("q3-c", "C", "重新展开第三条建议"))
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
                        question("q1", "第一段应该怎么写？",
                                option("q1-a", "A", "描述画面，并点出注意力被切碎的现象"),
                                option("q1-b", "B", "直接背诵一个万能开头"),
                                option("q1-c", "C", "只写个人经历")),
                        question("q2", "第二段最适合解释什么？",
                                option("q2-a", "A", "碎片化干扰为什么会降低学习质量"),
                                option("q2-b", "B", "手机品牌为什么受欢迎"),
                                option("q2-c", "C", "所有学生都不应该使用手机")),
                        question("q3", "结尾应该怎么收束？",
                                option("q3-a", "A", "提出平衡效率工具和深度专注的建议"),
                                option("q3-b", "B", "重复图画描述"),
                                option("q3-c", "C", "突然讨论就业压力"))
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
            return "第一段：描述图画/图表并点明现象。第二段：解释原因或影响，至少展开两点。第三段：总结观点并给出建议。";
        }
        return "第一段：说明写信目的。第二段：给出两条具体建议。第三段：礼貌收束，表达希望建议有帮助。";
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
