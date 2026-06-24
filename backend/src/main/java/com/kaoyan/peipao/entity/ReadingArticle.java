package com.kaoyan.peipao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reading_article")
public class ReadingArticle {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String articleKey;

    private String title;

    private String source;

    private String passage;

    private Integer difficulty;

    private String examType;

    private String yearLabel;

    private Integer translationLimit;

    private Integer active;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
