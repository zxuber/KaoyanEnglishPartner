package com.kaoyan.peipao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
}
