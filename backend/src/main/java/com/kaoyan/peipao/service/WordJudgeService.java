package com.kaoyan.peipao.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 单词本地判断引擎
 * 四级本地检查：直接包含 → 关键词匹配 → 近义词映射（同义词词林）→ 编辑距离
 */
@Slf4j
@Service
public class WordJudgeService {

    /** 同义词映射：从 cilin.txt 加载，word → 所有同义词集合 */
    private final Map<String, Set<String>> synonymDict = new HashMap<>();

    @PostConstruct
    public void loadSynonymDict() {
        ClassPathResource resource = new ClassPathResource("cilin.txt");
        int loaded = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String wordsStr = line.substring(eq + 1).trim();
                if (wordsStr.isEmpty()) continue;
                String[] words = wordsStr.split("\\s+");
                if (words.length < 2) continue;

                Set<String> group = new HashSet<>(Arrays.asList(words));
                for (String w : words) {
                    synonymDict.computeIfAbsent(w, k -> new HashSet<>()).addAll(group);
                }
                loaded++;
            }
            log.info("[SynonymDict] loaded {} synonym groups, {} unique words",
                    loaded, synonymDict.size());
        } catch (Exception e) {
            log.error("[SynonymDict] failed to load cilin.txt, synonym check will be disabled", e);
        }
    }

    /**
     * 判断用户回答是否正确
     * @param standardMeaning 单词标准释义
     * @param userAnswer 用户口述答案
     * @return 是否正确
     */

    /** 词性标注缩写 pattern：n. / vt. / vi. / adj. / adv. / prep. / conj. / pron. / v. / art. / num. / int. / aux. / abbr. */
    private static final Pattern POS_PATTERN =
            Pattern.compile("\\b(vt|vi|n|adj|adv|prep|conj|pron|v|art|num|int|aux|abbr)\\.", Pattern.CASE_INSENSITIVE);

    public boolean judge(String standardMeaning, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            log.info("[Judge] empty answer -> false");
            return false;
        }

        String std = normalize(standardMeaning);
        String ans = normalize(userAnswer);
        log.info("[Judge] raw meaning=[{}] normalized=[{}] | raw answer=[{}] normalized=[{}]",
                standardMeaning, std, userAnswer, ans);

        // 1. direct contains check
        if (std.contains(ans)) {
            log.info("[Judge] std contains ans -> true");
            return true;
        }
        if (ans.contains(std)) {
            log.info("[Judge] ans contains std -> true");
            return true;
        }

        // 2. keyword extraction
        List<String> keywords = extractKeywords(standardMeaning);
        log.info("[Judge] keywords: {}", keywords);

        for (String kw : keywords) {
            if (ans.contains(kw)) {
                log.info("[Judge] keyword [{}] in answer -> true", kw);
                return true;
            }
            if (isSynonym(kw, ans)) {
                log.info("[Judge] synonym of [{}] in answer -> true", kw);
                return true;
            }
            if (partialOverlap(kw, ans)) {
                log.info("[Judge] partial overlap [{}] <-> answer -> true", kw);
                return true;
            }
            if (editDistanceSimilar(kw, ans)) {
                log.info("[Judge] edit-dist on [{}] -> true", kw);
                return true;
            }
        }

        // 3. overall edit distance
        if (editDistanceSimilar(std, ans)) {
            log.info("[Judge] overall edit distance -> true");
            return true;
        }

        log.info("[Judge] no match -> false");
        return false;
    }


    /** 标准化文本：先整体剥离词性标注，再逐字符去标点 */
    private String normalize(String s) {
        return POS_PATTERN.matcher(s.toLowerCase()).replaceAll("")
                .replaceAll("[，。；：、！？《》（）\\[\\]【】\"\"''\\s]+", "")
                .replaceAll("[.,;:!?()\\[\\]{}'\"\\s]+", "")
                .trim();
    }

    /** 提取释义中的关键词 */
    private List<String> extractKeywords(String meaning) {
        List<String> keywords = new ArrayList<>();
        // 先整体移除词性标注（与 normalize 保持一致）
        String cleaned = POS_PATTERN.matcher(meaning).replaceAll("");
        // 按分号、逗号、空格分割
        String[] parts = cleaned.split("[，,;；\\s/]+");
        for (String part : parts) {
            String kw = part.trim();
            if (kw.length() >= 2 && !keywords.contains(kw)) {
                keywords.add(kw);
            }
        }
        return keywords;
    }

    /** 部分重叠匹配：如"难懂的"与"难以理解的"共享子串 */
    private boolean partialOverlap(String keyword, String answer) {
        if (keyword.length() < 3 || answer.length() < 2) return false;
        // Check if any 2-char substring of keyword appears in answer
        for (int i = 0; i <= keyword.length() - 2; i++) {
            String sub = keyword.substring(i, i + 2);
            if (answer.contains(sub)) return true;
        }
        return false;
    }

    /** 检查是否为近义词（从同义词词林查找） */
    private boolean isSynonym(String keyword, String answer) {
        Set<String> synonyms = synonymDict.get(keyword);
        if (synonyms != null) {
            for (String syn : synonyms) {
                if (!syn.equals(keyword) && answer.contains(syn)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 编辑距离相似度 */
    private boolean editDistanceSimilar(String a, String b) {
        if (a.length() < 2 || b.length() < 2) return false;
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen > 20) return false; // 太长的串不比较
        int distance = levenshtein(a, b);
        double similarity = 1.0 - (double) distance / maxLen;
        return similarity >= 0.65;
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }
}
