package com.kaoyan.peipao.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WordJudgeServiceTest {

    private final WordJudgeService service = new WordJudgeService();

    @Test
    void testExactMatch() {
        assertTrue(service.judge("n. 放弃", "放弃"));
    }

    @Test
    void testSynonym() {
        assertTrue(service.judge("vt. 放弃", "抛弃"));
    }

    @Test
    void testPartialAnswerMatch() {
        assertTrue(service.judge("n. 困难", "艰难困苦"));
    }

    @Test
    void testAnswerContainsMeaning() {
        assertTrue(service.judge("adj. 重要的", "重要的"));
    }

    @Test
    void testPosPrefixDoesNotContaminate() {
        // Bug fix: n. should be stripped as whole token, not leaving bare "n"
        assertTrue(service.judge("n. 痴迷，着魔", "痴迷着魔执念"));
    }

    @Test
    void testPosPrefixDoesNotContaminateExactAnswer() {
        assertTrue(service.judge("n. 痴迷，着魔", "痴迷着魔"));
    }

    @Test
    void testMultiplePOS() {
        assertTrue(service.judge("vt./vi. 分析", "剖析解析"));
    }

    @Test
    void testEmptyAnswer() {
        assertFalse(service.judge("n. 测试", ""));
        assertFalse(service.judge("n. 测试", null));
    }

    @Test
    void testCompletelyWrong() {
        assertFalse(service.judge("n. 苹果", "香蕉橘子"));
    }

    @Test
    void testPartialOverlapObscure() {
        assertTrue(service.judge("adj. 鲜为人知的；难以理解的 vt. 掩盖，使模糊，使隐晦", "晦涩的难懂的。"));
    }
}
