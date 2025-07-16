package com.roome.global.security.jwt.service;

import com.roome.global.security.jwt.exception.InvalidRefreshTokenException;
import com.roome.global.security.jwt.provider.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import static com.roome.global.security.jwt.util.TokenResponseUtil.addTokensToResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {

	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenService refreshTokenService;
    @Qualifier("blacklistRedisTemplate")
    private final RedisTemplate<String, Long> blacklistRedisTemplate;

	public void refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = extractAndValidateRefreshToken(request);
		Long userId = jwtTokenProvider.getUserFromRefreshToken(refreshToken);

		Map<String, String> stored = validateStoredToken(userId, refreshToken);
		validateClientFingerprint(userId, request, stored);

		Authentication authentication = jwtTokenProvider.getAuthenticationFromUserId(userId);

		String newAccessToken = jwtTokenProvider.createToken(authentication);
		String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

		refreshTokenService.deleteRefreshToken(userId);
		refreshTokenService.saveRefreshToken(userId, newRefreshToken, request);

		addTokensToResponse(response, newAccessToken, newRefreshToken);
	}

    public void addAccessTokenToBlacklist(String accessToken) {
        long remainingTime = jwtTokenProvider.getRemainingValidity(accessToken);

        blacklistRedisTemplate.opsForValue().set("blacklist:" + accessToken, 1L, Duration.ofSeconds(remainingTime));
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistRedisTemplate.hasKey("blacklist:" + token);
    }

	private String extractAndValidateRefreshToken(HttpServletRequest request) {
		String token = extractRefreshTokenFromCookie(request);
		if (token == null || !jwtTokenProvider.validateRefreshToken(token)) {
			throw new InvalidRefreshTokenException();
		}
		return token;
	}

	private Map<String, String> validateStoredToken(Long userId, String refreshToken) {
		Map<String, String> stored = refreshTokenService.getStoredFingerprint(userId);
		if (stored == null || !refreshToken.equals(stored.get("token"))) {
			throw new InvalidRefreshTokenException();
		}
		return stored;
	}

	private String extractRefreshTokenFromCookie(HttpServletRequest request) {
		if (request.getCookies() == null) return null;

		// 쿠키 검증 후 "refreshToken" 이름에 담겨 있는 refreshToken 반환
		for (Cookie cookie : request.getCookies()) {
			if ("refreshToken".equals(cookie.getName())) return cookie.getValue();
		}
		return null;
	}

	private void validateClientFingerprint(Long userId, HttpServletRequest request, Map<String, String> stored) {
		if (stored.isEmpty()) throw new InvalidRefreshTokenException();

		String storedIp = stored.get("ip");
		String storedUa = stored.get("userAgent");

		String requestIp = request.getRemoteAddr();
		String requestUa = request.getHeader("User-Agent");

		if (!Objects.equals(storedIp, requestIp) || !Objects.equals(storedUa, requestUa)) {
			log.warn("[환경 변경 감지] userId={}, oldIp={}, newIp={}, oldUA={}, newUA={}",
					userId, storedIp, requestIp, storedUa, requestUa);
		}
	}
}
