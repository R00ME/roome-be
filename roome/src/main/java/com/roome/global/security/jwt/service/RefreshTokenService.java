package com.roome.global.security.jwt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private static final Duration REFRES_TOKEN_DURATION = Duration.ofDays(14);
	@Qualifier("refreshTokenRedisTemplate")
	private final RedisTemplate<String, String> refreshTokenRedisTemplate;

	public void saveRefreshToken(Long userId, String refreshToken) {
		refreshTokenRedisTemplate.opsForValue().set("refresh:" + userId, refreshToken, REFRES_TOKEN_DURATION);
	}

	public String getRefreshToken(Long userId) {
		return refreshTokenRedisTemplate.opsForValue().get("refresh:" + userId);
	}

	// 현재는 만료 시간을 초기화 해주고 있음 -> 서버 관리로 가능함 -> 추후 해당 메서드로  redis 삭제를 해줄지 고민 중
	public void deleteRefreshToken(Long userId) {
		refreshTokenRedisTemplate.delete("refresh:" + userId);
	}

}
