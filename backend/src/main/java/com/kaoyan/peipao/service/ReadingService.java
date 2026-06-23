package com.kaoyan.peipao.service;

import com.kaoyan.peipao.dto.response.ReadingArticleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReadingService {

    private final LLMService llmService;

    public ReadingArticleResponse getMockArticle() {
        return ReadingArticleResponse.builder()
                .articleId("reading-001")
                .source("模拟外刊风格材料")
                .title("Why Deep Work Still Matters in a Distracted Age")
                .passage("""
                        In many modern workplaces, attention has become the scarcest resource. Workers are constantly interrupted by instant messages,
                        meetings, and the silent pressure to respond quickly. Although such responsiveness gives an impression of efficiency, it often
                        produces only shallow progress. Deep work, by contrast, requires extended periods of concentration in which a person focuses on
                        a cognitively demanding task without distraction. During these periods, the worker is not merely busy but is actually producing
                        something difficult and valuable.

                        The appeal of shallow work is easy to understand. It is visible, social, and measurable in the short term. A manager can see
                        who replies quickly; a colleague can appreciate who joins every discussion. Deep work is less visible. Its rewards usually appear
                        later, sometimes after days or weeks, and are therefore easy to underestimate. This difference helps explain why organizations
                        often praise focus in theory but reward distraction in practice.

                        Yet the economic value of concentration has not diminished. If anything, it has grown. As routine tasks are increasingly handled
                        by software, the tasks left to humans are those that demand judgment, synthesis, and creativity. These abilities cannot be fully
                        expressed in fragmented attention. A worker who cannot protect time for sustained thinking may remain active all day while creating
                        very little that distinguishes him from a machine.

                        This does not mean that communication should be rejected. The challenge is not to eliminate collaboration but to organize it more
                        intelligently. Teams can benefit from clear blocks of uninterrupted work, shared norms about response time, and deliberate moments
                        for discussion. In this sense, deep work is not an individual luxury. It is a structural discipline that allows modern knowledge
                        work to remain genuinely productive.
                        """)
                .questions(List.of(
                        ReadingArticleResponse.QuestionItem.builder()
                                .id("q1")
                                .stem("What is the main point of the passage?")
                                .focus("主旨概括")
                                .options(List.of(
                                        ReadingArticleResponse.OptionItem.builder().label("A").content("Deep work is a personal habit that matters only to highly creative individuals.").build(),
                                        ReadingArticleResponse.OptionItem.builder().label("B").content("Modern organizations should replace communication with long periods of total isolation.").build(),
                                        ReadingArticleResponse.OptionItem.builder().label("C").content("Deep, sustained concentration remains highly valuable and needs structural protection in modern work.").build(),
                                        ReadingArticleResponse.OptionItem.builder().label("D").content("Routine tasks are becoming more difficult because software interrupts human concentration.").build()
                                ))
                                .answer("The passage argues that deep, sustained concentration remains increasingly valuable and should be structurally protected in modern work.")
                                .explanation("首段定义 deep work，后两段说明其价值更高，末段提出组织层面的解决方案，整体主旨是强调深度工作仍然重要且需要制度保障。")
                                .build(),
                        ReadingArticleResponse.QuestionItem.builder()
                                .id("q2")
                                .stem("Why do organizations often reward distraction in practice?")
                                .focus("因果定位")
                                .options(List.of(
                                        ReadingArticleResponse.OptionItem.builder().label("A").content("Because deep work usually leads to conflict within teams and slows down communication.").build(),
                                        ReadingArticleResponse.OptionItem.builder().label("B").content("Because shallow work is more visible and immediately measurable than the delayed benefits of deep work.").build(),
                                        ReadingArticleResponse.OptionItem.builder().label("C").content("Because most managers believe software can fully replace human judgment and creativity.").build(),
                                        ReadingArticleResponse.OptionItem.builder().label("D").content("Because workers prefer cognitively demanding tasks to routine activities in the workplace.").build()
                                ))
                                .answer("Because shallow work is more visible and immediately measurable, while the benefits of deep work appear later and are easy to underestimate.")
                                .explanation("答案集中在第二段。作者对比 shallow work 与 deep work 的可见性和回报周期，这是题干里的 why 的直接原因。")
                                .build()
                ))
                .build();
    }

    public Map<String, Object> coachAnswer(String articleId, String questionId, String userAnswer, int turn) {
        ReadingArticleResponse article = getMockArticle();
        ReadingArticleResponse.QuestionItem question = article.getQuestions().stream()
                .filter(item -> item.getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("题目不存在"));

        String coachReply = llmService.generateReadingCoachReply(
                article.getTitle(),
                article.getPassage(),
                question.getStem(),
                question.getFocus(),
                question.getAnswer(),
                question.getExplanation(),
                userAnswer,
                turn
        );

        boolean reveal = turn >= 2;
        return Map.of(
                "coachReply", coachReply,
                "revealAnswer", reveal,
                "answer", reveal ? question.getAnswer() : "",
                "explanation", reveal ? question.getExplanation() : "",
                "turn", turn
        );
    }
}
