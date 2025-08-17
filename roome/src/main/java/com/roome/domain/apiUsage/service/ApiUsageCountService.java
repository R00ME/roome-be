package com.roome.domain.apiUsage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiUsageCountService {

    @Qualifier("apiCountRedisTemplate")
    private final RedisTemplate<String, Long> apiCountRedisTemplate;

    public void incrementCount(Long userId, String uri) {
        // userId + uri 조합으로 카운트 증가
        String key = "api_count:user:" + userId + ":" + uri;
        apiCountRedisTemplate.opsForValue().increment(key);
    }
}
