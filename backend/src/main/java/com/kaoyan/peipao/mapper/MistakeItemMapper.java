package com.kaoyan.peipao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
}
