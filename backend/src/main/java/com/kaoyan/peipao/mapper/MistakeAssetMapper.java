package com.kaoyan.peipao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kaoyan.peipao.dto.response.MistakeDoneResponse;
import com.kaoyan.peipao.dto.response.MistakeAssetResponse;
import com.kaoyan.peipao.entity.MistakeAsset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MistakeAssetMapper extends BaseMapper<MistakeAsset> {

    @Select("""
            SELECT
              a.id,
              a.category,
              a.subcategory,
              a.source_text AS sourceText,
              a.translation,
              a.source_hint AS sourceHint,
              a.note,
              a.example_en AS exampleEn,
              a.example_zh AS exampleZh,
              a.source_module AS sourceModule,
              COALESCE(p.status, 'active') AS status
            FROM mistake_asset_library a
            LEFT JOIN mistake_asset_progress p
              ON p.asset_id = a.id
             AND p.user_id = #{userId}
            WHERE a.category = #{category}
              AND a.active = 1
              AND (p.status IS NULL OR p.status <> 'done')
            ORDER BY a.sort_order ASC, a.id ASC
            """)
    List<MistakeAssetResponse> selectVisibleByUserAndCategory(@Param("userId") Long userId, @Param("category") String category);

    @Select("""
            SELECT
              CONCAT('asset-', a.id) AS id,
              'asset' AS sourceType,
              a.category,
              CASE a.category
                WHEN 'writing' THEN '写作表达'
                WHEN 'phrase' THEN '固定搭配'
                WHEN 'confusion' THEN '易混词'
                ELSE a.category
              END AS categoryLabel,
              a.source_text AS sourceText,
              a.translation,
              a.source_module AS sourceModule,
              a.source_hint AS sourceHint,
              a.note,
              p.status,
              DATE_FORMAT(p.updated_at, '%Y-%m-%dT%H:%i:%s') AS doneAt
            FROM mistake_asset_progress p
            JOIN mistake_asset_library a ON a.id = p.asset_id
            WHERE p.user_id = #{userId}
              AND p.status = 'done'
              AND a.active = 1
            ORDER BY p.updated_at DESC, a.sort_order ASC, a.id ASC
            """)
    List<MistakeDoneResponse> selectDoneCards(@Param("userId") Long userId);
}
