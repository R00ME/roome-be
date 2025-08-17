package com.roome.domain.apiUsage.service;

import com.roome.domain.apiUsage.repository.UserApiUsageRepository;
import com.roome.domain.apiUsage.entity.UserApiUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApiUsageService {

    @Qualifier("apiCountRedisTemplate")
    private final RedisTemplate<String, Long> apiCountRedisTemplate;
    private final UserApiUsageRepository userApiUsageRepository;

    public void flushCountsToDb() {
        // 1. Redis 키 스캔 (api_count:*)
        Set<String> keys = apiCountRedisTemplate.keys("api_count:*");

        if (keys == null) return;

        for (String key : keys) {
            long count = apiCountRedisTemplate.opsForValue().get(key);

            String[] parts = key.split(":");
            Long userId = Long.parseLong(parts[2]);
            String uri = parts[3];
            userApiUsageRepository.save(
                    UserApiUsage.builder()
                            .userId(userId)
                            .domain(resolveDomain(uri))
                            .apiUri(uri)
                            .date(LocalDate.now())
                            .count(count)
                            .build()
            );
        }
    }

    private String resolveDomain(String uri) {
        // "/api/auth/login" → ["", "api", "auth", "login"]
        String[] parts = uri.split("/");
        if (parts.length >= 3) {
            return parts[2];
        }
        return "etc";
    }
}
