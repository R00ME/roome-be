package com.roome.global.security.jwt.service;

import com.roome.global.security.jwt.dto.GetAccessTokenByTempCodeRequest;
import com.roome.global.security.jwt.provider.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

import static com.roome.global.security.jwt.util.TokenResponseUtil.addTokensToResponse;

@Service
@RequiredArgsConstructor
public class TokenExchangeService {

	private static final long EXPIRATION_MINUTES = 3;
	private static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(14);
	@Qualifier("tempCodeRedisTemplate")
	private final RedisTemplate<String, String> tempCodeRedisTemplate;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenService refreshTokenService;

	public String generateTempCode(String accessToken) {
		String tempCode = UUID.randomUUID().toString();
		tempCodeRedisTemplate.opsForValue().set(tempCode, accessToken, Duration.ofMinutes(EXPIRATION_MINUTES));
		return tempCode;
	}

	public void exchangeTempCode(GetAccessTokenByTempCodeRequest getAccessTokenByTempCodeRequest, HttpServletRequest request,
								 HttpServletResponse response) {
		String accessToken = getAccessTokenByTempCode(getAccessTokenByTempCodeRequest.getTempCode());
		Long userId = jwtTokenProvider.getUserIdFromAccessToken(accessToken);

		// refreshToken 생성
		String refreshToken = jwtTokenProvider.createRefreshToken(userId);

		// refreshToken redis 에 저장
		refreshTokenService.saveRefreshToken(userId, refreshToken, request);

		// accessToken: Header로 전달
		response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

		addTokensToResponse(response, accessToken, refreshToken);
	}

	private String getAccessTokenByTempCode(String tempCode) {
		String accessTokenFromRedis = tempCodeRedisTemplate.opsForValue().get(tempCode);
		tempCodeRedisTemplate.delete(tempCode);
		return accessTokenFromRedis;
	}
}
