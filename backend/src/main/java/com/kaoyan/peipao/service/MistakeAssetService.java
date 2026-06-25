package com.kaoyan.peipao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kaoyan.peipao.common.BizException;
import com.kaoyan.peipao.common.ErrorCode;
import com.kaoyan.peipao.dto.response.MistakeAssetResponse;
import com.kaoyan.peipao.entity.MistakeAsset;
import com.kaoyan.peipao.mapper.MistakeAssetMapper;
import com.kaoyan.peipao.mapper.MistakeAssetProgressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MistakeAssetService {

    private static final Set<String> SUPPORTED_CATEGORIES = Set.of("writing", "phrase", "confusion");

    private final MistakeAssetMapper mistakeAssetMapper;
    private final MistakeAssetProgressMapper mistakeAssetProgressMapper;

    public List<MistakeAssetResponse> listByCategory(Long userId, String category) {
        return mistakeAssetMapper.selectVisibleByUserAndCategory(userId, normalizeCategory(category));
    }

    public void markDone(Long userId, Long assetId) {
        MistakeAsset asset = mistakeAssetMapper.selectOne(new LambdaQueryWrapper<MistakeAsset>()
                .eq(MistakeAsset::getId, assetId)
                .eq(MistakeAsset::getActive, 1)
                .last("LIMIT 1"));
        if (asset == null) {
            throw new BizException(ErrorCode.MISTAKE_ASSET_NOT_FOUND);
        }
        mistakeAssetProgressMapper.upsertStatus(userId, assetId, "done");
    }

    private String normalizeCategory(String category) {
        String normalized = category == null ? "" : category.trim().toLowerCase();
        if (!SUPPORTED_CATEGORIES.contains(normalized)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "暂不支持的误解本资产分类");
        }
        return normalized;
    }
}
