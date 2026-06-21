package com.kaoyan.peipao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("word_progress")
public class WordProgress {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long wordId;
    private String status;      // new / learning / mastered
    private Integer mistakeCount;
    private Integer correctStreak;
    private LocalDate nextReviewDate;
    private Integer reviewInterval;
    private LocalDateTime lastReviewedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
