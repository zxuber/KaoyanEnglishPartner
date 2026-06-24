package com.kaoyan.peipao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kaoyan.peipao.entity.ReadingArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReadingArticleMapper extends BaseMapper<ReadingArticle> {

    @Select("SELECT * FROM reading_article WHERE active = 1 ORDER BY id LIMIT 1")
    ReadingArticle selectActiveArticle();

    @Select("SELECT * FROM reading_article WHERE article_key = #{articleKey} LIMIT 1")
    ReadingArticle selectByArticleKey(String articleKey);
}
