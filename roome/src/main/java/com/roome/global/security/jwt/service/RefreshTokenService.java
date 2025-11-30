package com.roome.global.security.jwt.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RefreshTokenService {

	private static final Duration REFRES_TOKEN_DURATION = Duration.ofDays(14);
	private final RedisTemplate<String, String> refreshTokenRedisTemplate;

	public RefreshTokenService(
			@Qualifier("refreshTokenRedisTemplate")
			RedisTemplate<String, String> refreshTokenRedisTemplate
	) {
		this.refreshTokenRedisTemplate = refreshTokenRedisTemplate;
	}

	public void saveRefreshToken(Long userId, String refreshToken, HttpServletRequest request) {
		String ip = request.getRemoteAddr();
		String userAgent = request.getHeader("User-Agent");

		Map<String, String> stored = Map.of(
				"token", refreshToken,
				"ip", ip,
				"userAgent", userAgent
		);

		refreshTokenRedisTemplate.opsForHash().putAll("refreshToken:" + userId, stored);
		refreshTokenRedisTemplate.expire("refreshToken:" + userId, REFRES_TOKEN_DURATION);
	}

	public Map<String, String> getStoredFingerprint(Long userId) {
		Map<Object, Object> stored = refreshTokenRedisTemplate.opsForHash().entries("refreshToken:" + userId);
		if (stored.isEmpty()) return null;

		// Redis에서 가져온 Map<Object, Object> → Map<String, String> 으로 변환
		return stored.entrySet().stream()
				.collect(Collectors.toMap(
						e -> String.valueOf(e.getKey()),
						e -> String.valueOf(e.getValue())
				));
	}

	// 현재는 만료 시간을 초기화 해주고 있음 -> 서버 관리로 가능함 -> 추후 해당 메서드로  redis 삭제를 해줄지 고민 중
	public void deleteRefreshToken(Long userId) {
		refreshTokenRedisTemplate.delete("refreshToken:" + userId);
	}
}
