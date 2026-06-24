package com.kaoyan.peipao.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // 400xx: Client errors
    PARAM_INVALID(40001, "参数校验失败"),
    TARGET_SCORE_OUT_OF_RANGE(40002, "目标分数超出范围（40-80）"),

    // 401xx: Auth errors
    UNAUTHORIZED(40101, "未登录或登录已过期"),
    INVALID_TOKEN(40102, "无效的登录凭证"),

    // 404xx: Not found
    USER_NOT_FOUND(40401, "用户不存在"),
    WORD_NOT_FOUND(40402, "单词不存在"),
    READING_NOT_FOUND(40403, "阅读篇目不存在"),
    READING_QUESTION_NOT_FOUND(40404, "阅读题目不存在"),
    MISTAKE_ITEM_NOT_FOUND(40405, "误解本条目不存在"),

    // 500xx: Server errors
    INTERNAL_ERROR(50001, "服务器内部错误"),
    READING_TRANSLATION_LIMIT_REACHED(50002, "每篇最多5次翻译机会哦～"),
    LLM_TIMEOUT(50301, "AI 服务响应超时，已使用本地降级方案"),
    LLM_GENERATION_FAILED(50302, "AI 方案生成失败，请重试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
