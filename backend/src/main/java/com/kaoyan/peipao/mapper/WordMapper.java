package com.kaoyan.peipao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kaoyan.peipao.entity.Word;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface WordMapper extends BaseMapper<Word> {
    @Select("SELECT * FROM word WHERE unit = #{unit} ORDER BY word_index")
    List<Word> selectByUnit(int unit);

    @Select("SELECT * FROM word ORDER BY RAND() LIMIT #{count}")
    List<Word> selectRandom(int count);
}
