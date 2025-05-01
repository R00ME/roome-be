package com.roome.global.jwt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TempTokenService {

    @Qualifier("tempCodeRedisTemplate")
    private final RedisTemplate<String, String> tempCodeRedisTemplate;
    private static final long EXPIRATION_MINUTES = 3;

    public String generateTempCode(String accessToken) {
        String tempCode = UUID.randomUUID().toString();
        tempCodeRedisTemplate.opsForValue().set(tempCode, accessToken, Duration.ofMinutes(EXPIRATION_MINUTES));
        return tempCode;
    }

    public String getAccessTokenByTempCode(String tempCode) {
        String accessTokenFromRedis = tempCodeRedisTemplate.opsForValue().get(tempCode);
        if(accessTokenFromRedis == null) throw new IllegalArgumentException("유효하지 않은 임시 코드입니다.");
        tempCodeRedisTemplate.delete(tempCode);
        return accessTokenFromRedis;
    }
}
