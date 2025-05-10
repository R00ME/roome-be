package com.roome.global.security.jwt.service;

import com.roome.global.security.jwt.exception.InvalidRefreshTokenException;
import com.roome.global.security.jwt.token.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenService refreshTokenService;


	public void refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = extractRefreshTokenFromCookie(request);

		if (refreshToken == null || !jwtTokenProvider.validateRefreshToken(refreshToken))
			throw new InvalidRefreshTokenException();

		Long userId = jwtTokenProvider.getUserFromRefreshToken(refreshToken);
		System.out.println("userId: " + userId);
		String savedToken = refreshTokenService.getRefreshToken(userId);
		if (!refreshToken.equals(savedToken)) throw new InvalidRefreshTokenException();

		Authentication authentication = jwtTokenProvider.getAuthenticationFromUserId(userId);
		String newAccessToken = jwtTokenProvider.createToken(authentication);

		response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken);
	}

	private String extractRefreshTokenFromCookie(HttpServletRequest request) {
		if (request.getCookies() == null) return null;

		for (Cookie cookie : request.getCookies()) {
			if ("refreshToken".equals(cookie.getName())) return cookie.getValue();
		}
		return null;
	}
}
