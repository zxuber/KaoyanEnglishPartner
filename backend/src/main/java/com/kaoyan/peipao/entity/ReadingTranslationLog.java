package com.kaoyan.peipao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reading_translation_log")
public class ReadingTranslationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long articleId;

    private String contentType;

    private String sourceText;

    private String translatedText;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
