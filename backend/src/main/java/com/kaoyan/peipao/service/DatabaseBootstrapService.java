package com.kaoyan.peipao.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kaoyan.peipao.entity.ReadingArticle;
import com.kaoyan.peipao.entity.ReadingQuestion;
import com.kaoyan.peipao.mapper.ReadingArticleMapper;
import com.kaoyan.peipao.mapper.ReadingQuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseBootstrapService implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final ReadingArticleMapper readingArticleMapper;
    private final ReadingQuestionMapper readingQuestionMapper;

    @Override
    public void run(ApplicationArguments args) {
        createTables();
        seedReadingData();
    }

    private void createTables() {
        List<String> ddlList = List.of(
                """
                CREATE TABLE IF NOT EXISTS reading_article (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  article_key VARCHAR(64) NOT NULL UNIQUE,
                  title VARCHAR(255) NOT NULL,
                  source VARCHAR(255) NULL,
                  passage TEXT NOT NULL,
                  difficulty INT DEFAULT 3,
                  exam_type VARCHAR(32) DEFAULT 'english-1',
                  year_label VARCHAR(32) DEFAULT 'mock',
                  translation_limit INT DEFAULT 5,
                  active TINYINT DEFAULT 1,
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS reading_question (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  article_id BIGINT NOT NULL,
                  question_no INT NOT NULL,
                  stem VARCHAR(500) NOT NULL,
                  focus VARCHAR(128) NULL,
                  option_a VARCHAR(500) NOT NULL,
                  option_b VARCHAR(500) NOT NULL,
                  option_c VARCHAR(500) NOT NULL,
                  option_d VARCHAR(500) NOT NULL,
                  correct_option VARCHAR(8) NOT NULL,
                  answer TEXT NOT NULL,
                  explanation TEXT NOT NULL,
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  KEY idx_reading_question_article (article_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS reading_record (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  user_id BIGINT NOT NULL,
                  article_id BIGINT NOT NULL,
                  question_id BIGINT NOT NULL,
                  selected_option VARCHAR(8) NULL,
                  user_reasoning TEXT NULL,
                  coach_reply TEXT NULL,
                  turn INT DEFAULT 1,
                  reveal_answer TINYINT DEFAULT 0,
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  KEY idx_reading_record_user_article (user_id, article_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS reading_translation_log (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  user_id BIGINT NOT NULL,
                  article_id BIGINT NOT NULL,
                  content_type VARCHAR(16) NOT NULL,
                  source_text TEXT NOT NULL,
                  translated_text TEXT NOT NULL,
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                  KEY idx_reading_translation_user_article (user_id, article_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS mistake_item (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  user_id BIGINT NOT NULL,
                  type VARCHAR(16) NOT NULL,
                  source_text TEXT NOT NULL,
                  translation TEXT NOT NULL,
                  source_module VARCHAR(32) DEFAULT '阅读',
                  article_key VARCHAR(64) NULL,
                  status VARCHAR(16) DEFAULT 'active',
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  KEY idx_mistake_user_type (user_id, type, status)
                )
                """
        );
        ddlList.forEach(jdbcTemplate::execute);
    }

    private void seedReadingData() {
        Long count = readingArticleMapper.selectCount(new QueryWrapper<>());
        if (count != null && count > 0) {
            return;
        }

        ReadingArticle article = new ReadingArticle();
        article.setArticleKey("reading-001");
        article.setTitle("Why Deep Work Still Matters in a Distracted Age");
        article.setSource("模拟外刊风格材料");
        article.setPassage("""
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
                """);
        article.setDifficulty(3);
        article.setExamType("english-1");
        article.setYearLabel("mock");
        article.setTranslationLimit(5);
        article.setActive(1);
        readingArticleMapper.insert(article);

        ReadingQuestion q1 = new ReadingQuestion();
        q1.setArticleId(article.getId());
        q1.setQuestionNo(1);
        q1.setStem("What is the main point of the passage?");
        q1.setFocus("主旨概括");
        q1.setOptionA("Deep work is a personal habit that matters only to highly creative individuals.");
        q1.setOptionB("Modern organizations should replace communication with long periods of total isolation.");
        q1.setOptionC("Deep, sustained concentration remains highly valuable and needs structural protection in modern work.");
        q1.setOptionD("Routine tasks are becoming more difficult because software interrupts human concentration.");
        q1.setCorrectOption("C");
        q1.setAnswer("The passage argues that deep, sustained concentration remains increasingly valuable and should be structurally protected in modern work.");
        q1.setExplanation("首段定义 deep work，后两段说明其价值更高，末段提出组织层面的解决方案，整体主旨是强调深度工作仍然重要且需要制度保障。");
        readingQuestionMapper.insert(q1);

        ReadingQuestion q2 = new ReadingQuestion();
        q2.setArticleId(article.getId());
        q2.setQuestionNo(2);
        q2.setStem("Why do organizations often reward distraction in practice?");
        q2.setFocus("因果定位");
        q2.setOptionA("Because deep work usually leads to conflict within teams and slows down communication.");
        q2.setOptionB("Because shallow work is more visible and immediately measurable than the delayed benefits of deep work.");
        q2.setOptionC("Because most managers believe software can fully replace human judgment and creativity.");
        q2.setOptionD("Because workers prefer cognitively demanding tasks to routine activities in the workplace.");
        q2.setCorrectOption("B");
        q2.setAnswer("Because shallow work is more visible and immediately measurable, while the benefits of deep work appear later and are easy to underestimate.");
        q2.setExplanation("答案集中在第二段。作者对比 shallow work 与 deep work 的可见性和回报周期，这是题干里的 why 的直接原因。");
        readingQuestionMapper.insert(q2);
    }
}
