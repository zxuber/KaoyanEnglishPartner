package com.kaoyan.peipao.service;

import com.kaoyan.peipao.dto.response.MistakeDoneResponse;
import com.kaoyan.peipao.mapper.MistakeItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MistakeDoneService {

    private final MistakeItemMapper mistakeItemMapper;

    public List<MistakeDoneResponse> listDone(Long userId) {
        List<MistakeDoneResponse> result = mistakeItemMapper.selectDoneCards(userId);
        log.info("[已掌握] 查询DONE单词 userId={}, total={}", userId, result.size());
        return result;
    }
}
