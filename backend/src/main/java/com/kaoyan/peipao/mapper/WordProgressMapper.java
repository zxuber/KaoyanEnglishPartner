package com.kaoyan.peipao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kaoyan.peipao.entity.WordProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface WordProgressMapper extends BaseMapper<WordProgress> {
    @Select("""
        SELECT wp.* FROM word_progress wp
        WHERE wp.user_id = #{userId}
          AND wp.next_review_date <= CURDATE()
          AND wp.status != 'mastered'
        ORDER BY wp.next_review_date ASC
        LIMIT #{limit}
    """)
    List<WordProgress> selectDueReviews(Long userId, int limit);

    @Select("SELECT COUNT(*) FROM word_progress WHERE user_id = #{userId} AND status = 'mastered'")
    int countMastered(Long userId);

    @Select("SELECT COUNT(*) FROM word_progress WHERE user_id = #{userId}")
    int countTotal(Long userId);

    @Select("SELECT * FROM word_progress WHERE user_id = #{userId} AND word_id = #{wordId}")
    WordProgress selectByUserAndWord(Long userId, Long wordId);
}
