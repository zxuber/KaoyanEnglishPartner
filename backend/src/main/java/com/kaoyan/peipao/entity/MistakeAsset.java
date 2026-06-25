package com.kaoyan.peipao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mistake_asset_library")
public class MistakeAsset {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String category;

    private String subcategory;

    private String sourceText;

    private String translation;

    private String sourceHint;

    private String note;

    private String exampleEn;

    private String exampleZh;

    private String sourceModule;

    private Integer sortOrder;

    private Integer active;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
