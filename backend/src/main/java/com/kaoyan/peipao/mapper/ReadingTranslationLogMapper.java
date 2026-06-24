package com.kaoyan.peipao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kaoyan.peipao.entity.ReadingTranslationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReadingTranslationLogMapper extends BaseMapper<ReadingTranslationLog> {

    @Select("SELECT COUNT(1) FROM reading_translation_log WHERE user_id = #{userId} AND article_id = #{articleId} AND session_id = #{sessionId} AND content_type = #{contentType}")
    int countByUserAndArticleAndSessionAndType(Long userId, Long articleId, String sessionId, String contentType);
}
