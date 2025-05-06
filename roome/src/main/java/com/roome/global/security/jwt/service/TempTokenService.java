package com.roome.global.security.jwt.service;

import com.roome.global.security.jwt.dto.GetAccessTokenByTempCodeRequest;
import com.roome.global.security.jwt.token.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TempTokenService {

	private static final long EXPIRATION_MINUTES = 3;
	@Qualifier("tempCodeRedisTemplate")
	private final RedisTemplate<String, String> tempCodeRedisTemplate;
	private final JwtTokenProvider jwtTokenProvider;

	public String generateTempCode(String accessToken) {
		String tempCode = UUID.randomUUID().toString();
		tempCodeRedisTemplate.opsForValue().set(tempCode, accessToken, Duration.ofMinutes(EXPIRATION_MINUTES));
		return tempCode;
	}

	public void exchangeTempCode(GetAccessTokenByTempCodeRequest getAccessTokenByTempCodeRequest,
								 HttpServletResponse response) {
		String accessToken = getAccessTokenByTempCode(getAccessTokenByTempCodeRequest.getTempCode());

		String refreshToken = jwtTokenProvider.createRefreshToken(jwtTokenProvider.getUserIdFromAccessToken(accessToken));

		// accessToken: Header로 전달
		response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

		// refreshToken: Cookie로 전달
		ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
				.httpOnly(true)
				.secure(true)
				.sameSite("None")
				.path("/")
				.maxAge(Duration.ofDays(14))
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	private String getAccessTokenByTempCode(String tempCode) {
		String accessTokenFromRedis = tempCodeRedisTemplate.opsForValue().get(tempCode);
		if (accessTokenFromRedis == null) throw new IllegalArgumentException("유효하지 않은 임시 코드입니다.");
		tempCodeRedisTemplate.delete(tempCode);
		return accessTokenFromRedis;
	}
}
