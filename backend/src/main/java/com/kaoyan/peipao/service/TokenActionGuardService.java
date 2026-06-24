package com.kaoyan.peipao.service;

import com.kaoyan.peipao.common.BizException;
import com.kaoyan.peipao.common.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenActionGuardService {

    private final Map<String, Long> lastActionAtMap = new ConcurrentHashMap<>();

    public void guard(Long userId, String actionType, Duration cooldown) {
        long now = System.currentTimeMillis();
        String key = userId + ":" + actionType;
        Long previous = lastActionAtMap.put(key, now);
        if (previous != null && now - previous < cooldown.toMillis()) {
            lastActionAtMap.put(key, previous);
            throw new BizException(ErrorCode.TOKEN_ACTION_TOO_FREQUENT);
        }
    }
}
