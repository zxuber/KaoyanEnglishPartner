package com.kaoyan.peipao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kaoyan.peipao.entity.ReadingQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReadingQuestionMapper extends BaseMapper<ReadingQuestion> {

    @Select("SELECT * FROM reading_question WHERE article_id = #{articleId} ORDER BY question_no ASC")
    List<ReadingQuestion> selectByArticleId(Long articleId);
}
