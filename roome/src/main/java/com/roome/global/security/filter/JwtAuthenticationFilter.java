package com.roome.global.security.filter;

import com.roome.global.security.jwt.exception.InvalidJwtTokenException;
import com.roome.global.security.jwt.provider.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends GenericFilterBean { // JwtFilter 요청마다 JWT 검증

	private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
	private final JwtTokenProvider jwtTokenProvider;
	@Qualifier("blacklistRedisTemplate")
	private final RedisTemplate<String, String> blacklistRedisTemplate;

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException, IOException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
		String jwt = jwtTokenProvider.resolveToken(httpServletRequest);
		String requestURI = httpServletRequest.getRequestURI();

		if (StringUtils.hasText(jwt)) {
			// 블랙리스트 확인
			if (blacklistRedisTemplate.hasKey("blacklist:" + jwt)) {
				logger.warn("블랙리스트에 등록된 토큰입니다. uri: {}", requestURI);
				throw new InvalidJwtTokenException();
			}

			// 유효성 검사 후 인증 객체 설정
			if (jwtTokenProvider.validateToken(jwt)) {
				Authentication authentication = jwtTokenProvider.getAuthentication(jwt);
				SecurityContextHolder.getContext().setAuthentication(authentication);
				logger.debug("Security Context에 '{}' 인증 정보를 저장했습니다, uri: {}", authentication.getName(), requestURI);
			} else {
				logger.debug("JWT 토큰이 유효하지 않습니다, uri: {}", requestURI);
				throw new InvalidJwtTokenException();
			}
		} else {
			logger.debug("JWT 토큰이 없습니다, uri: {}", requestURI);
		}

		filterChain.doFilter(servletRequest, servletResponse);
	}
}
