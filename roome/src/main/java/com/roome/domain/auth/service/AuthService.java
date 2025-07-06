package com.roome.domain.auth.service;

import com.roome.global.security.jwt.service.RefreshTokenService;
import com.roome.global.security.jwt.provider.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final JwtTokenProvider jwtTokenProvider;

	@Qualifier("blacklistRedisTemplate")
	private final RedisTemplate<String, String> blacklistRedisTemplate;
	private final RefreshTokenService refreshTokenService;

	public void logout(Long userId, HttpServletRequest request, HttpServletResponse response) {

		String accessToken = jwtTokenProvider.resolveToken(request);
		if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {

			// 1. RefreshToken 삭제
			refreshTokenService.deleteRefreshToken(userId);

			// 2. AccessToken 블랙리스트 등록
			long expiration = jwtTokenProvider.getExpiration(accessToken);
			blacklistRedisTemplate.opsForValue().set("blacklist:" + accessToken, "logout", expiration, TimeUnit.MILLISECONDS);
		}

		// 쿠키 refreshToken 삭젠
		ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
				.httpOnly(true)
				.secure(true)
				.sameSite("None")
				.path("/")
				.maxAge(0)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
	}
}
