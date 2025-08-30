package com.roome.domain.auth.service;

import com.roome.global.security.jwt.service.RefreshTokenService;
import com.roome.global.security.jwt.provider.JwtTokenProvider;
import com.roome.global.security.jwt.service.TokenService;
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
	private final RedisTemplate<String, Long> blacklistRedisTemplate;
	private final RefreshTokenService refreshTokenService;
	private final TokenService tokenService;

	public void logout(Long userId, HttpServletRequest request, HttpServletResponse response) {

		String accessToken = jwtTokenProvider.resolveToken(request);
		if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {

			// 1. RefreshToken 삭제
			refreshTokenService.deleteRefreshToken(userId);

			// 2. AccessToken 블랙리스트 등록
			tokenService.addAccessTokenToBlacklist(accessToken);
		}

		// 쿠키 refreshToken 삭제
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
