package com.kaoyan.peipao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kaoyan.peipao.entity.MistakeAssetProgress;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MistakeAssetProgressMapper extends BaseMapper<MistakeAssetProgress> {

    @Insert("""
            INSERT INTO mistake_asset_progress (user_id, asset_id, status)
            VALUES (#{userId}, #{assetId}, #{status})
            ON DUPLICATE KEY UPDATE
              status = VALUES(status),
              updated_at = CURRENT_TIMESTAMP
            """)
    void upsertStatus(@Param("userId") Long userId, @Param("assetId") Long assetId, @Param("status") String status);
}
