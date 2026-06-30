package com.kaoyan.peipao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("practice_record")
public class PracticeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String module;

    private String questionKey;

    private String title;

    private String summary;

    private String selectedOption;

    private String userAnswer;

    private String coachReply;

    private Integer completed;

    private Integer important;

    private LocalDateTime trainedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
