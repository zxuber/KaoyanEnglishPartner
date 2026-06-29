package com.kaoyan.peipao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kaoyan.peipao.dto.response.MistakeDoneResponse;
import com.kaoyan.peipao.entity.MistakeItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MistakeItemMapper extends BaseMapper<MistakeItem> {

    @Select("SELECT * FROM mistake_item WHERE user_id = #{userId} AND type = #{type} AND status = 'active' ORDER BY updated_at DESC, id DESC")
    List<MistakeItem> selectActiveByUserAndType(Long userId, String type);

    @Select("SELECT * FROM mistake_item WHERE user_id = #{userId} AND type = #{type} AND source_text = #{sourceText} AND status = 'active' LIMIT 1")
    MistakeItem selectActiveBySource(Long userId, String type, String sourceText);

    @Select("SELECT * FROM mistake_item WHERE user_id = #{userId} AND type = 'word' AND status = 'active' ORDER BY updated_at DESC, id DESC LIMIT #{limit}")
    List<MistakeItem> selectActiveWordMistakes(Long userId, int limit);

    @Select("""
            SELECT
              CONCAT('mistake-', id) AS id,
              'mistake' AS sourceType,
              type AS category,
              CASE type
                WHEN 'word' THEN '单词'
                WHEN 'sentence' THEN '短句'
                ELSE type
              END AS categoryLabel,
              source_text AS sourceText,
              translation,
              source_module AS sourceModule,
              '' AS sourceHint,
              '' AS note,
              status,
              DATE_FORMAT(updated_at, '%Y-%m-%dT%H:%i:%s') AS doneAt
            FROM mistake_item
            WHERE user_id = #{userId}
              AND status = 'done'
              AND type = 'word'
            ORDER BY updated_at DESC, id DESC
            """)
    List<MistakeDoneResponse> selectDoneCards(Long userId);
}
