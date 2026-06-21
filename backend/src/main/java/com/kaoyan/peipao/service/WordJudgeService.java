package com.kaoyan.peipao.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 单词本地判断引擎
 * 优先本地判定（近义词+编辑距离），不调 LLM
 */
@Slf4j
@Service
public class WordJudgeService {

    /** 常用近义词映射 */
    private static final Map<String, Set<String>> SYNONYM_MAP = new HashMap<>();
    static {
        add("放弃", "抛弃", "遗弃", "丢弃", "舍弃", "摒弃");
        add("重要的", "重大的", "关键的", "主要的", "显著的");
        add("分析", "剖析", "解析", "研究");
        add("方法", "方式", "途径", "手段", "办法");
        add("增加", "增长", "提升", "提高", "增强", "增多");
        add("减少", "降低", "下降", "削减", "缩小", "减弱");
        add("显示", "表明", "说明", "揭示", "展现", "呈现");
        add("获得", "获取", "得到", "取得", "赢得");
        add("提供", "给予", "供给", "供应", "赋予");
        add("完成", "实现", "达成", "达到", "完成");
        add("困难", "艰难", "艰巨", "困苦", "不易");
        add("复杂的", "繁琐的", "错综复杂的");
        add("快速的", "迅速的", "敏捷的", "高速的");
        add("巨大的", "庞大的", "宏大的", "浩大的");
        add("影响", "作用", "效果", "效应");
        add("证据", "证明", "凭证", "依据");
        add("争论", "争议", "辩论", "争执");
        add("建立", "创立", "创建", "设立", "成立");
        add("包含", "包括", "含有", "涵盖", "囊括");
        add("表明", "表示", "显示", "说明", "暗示");
        add("变化", "改变", "转变", "转换", "变更");
        add("发展", "进展", "进步", "进化", "成长");
        add("环境", "周围", "周边", "四周", "氛围");
        add("特点", "特征", "特色", "特质", "特性");
        add("提倡", "倡导", "主张", "拥护", "支持");
        add("限制", "约束", "制约", "局限", "束缚");
        add("扩大", "扩展", "扩张", "拓宽", "放大");
        add("缩小", "减少", "缩减", "压缩", "精简");
    }

    private static void add(String... words) {
        Set<String> all = new HashSet<>(Arrays.asList(words));
        for (String w : words) {
            SYNONYM_MAP.computeIfAbsent(w, k -> new HashSet<>()).addAll(all);
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

    /** 检查是否为近义词 */
    private boolean isSynonym(String keyword, String answer) {
        Set<String> synonyms = SYNONYM_MAP.get(keyword);
        if (synonyms != null) {
            for (String syn : synonyms) {
                if (answer.contains(syn)) return true;
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
