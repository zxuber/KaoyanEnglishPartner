package com.kaoyan.peipao.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("[异常] validation failed: {}", msg);
        return ResponseEntity.badRequest().body(Result.fail(ErrorCode.PARAM_INVALID.getCode(), msg));
    }

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException e) {
        log.warn("[异常] biz code={} message={}", e.getCode(), e.getMessage());
        return ResponseEntity.ok(Result.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("[异常] unhandled error", e);
        return ResponseEntity.internalServerError()
                .body(Result.fail(ErrorCode.INTERNAL_ERROR.getCode(), e.getMessage()));
    }
}
