package com.roome.global.jwt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Qualifier("refreshTokenRedisTemplate")
    private final RedisTemplate<String, String> refreshTokenRedisTemplate;
    private static final Duration REFRES_TOKEN_DURATION = Duration.ofDays(14);

    public void saveRefreshToken(Long userId, String refreshToken) {
        refreshTokenRedisTemplate.opsForValue().set("refresh:" + userId, refreshToken, REFRES_TOKEN_DURATION);
    }

    public String getRefreshToken(Long userId) {
        return refreshTokenRedisTemplate.opsForValue().get("refresh:" + userId);
    }

    public void deleteRefreshToken(Long userId) {
        refreshTokenRedisTemplate.delete("refresh:" + userId);
    }

}
