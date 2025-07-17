package com.roome.global.security.filter;

import com.roome.global.security.jwt.provider.JwtTokenProvider;
import com.roome.global.security.jwt.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends GenericFilterBean { // JwtFilter 요청마다 JWT 검증

	private final JwtTokenProvider jwtTokenProvider;
	private final TokenService tokenService;

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {

		HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
		String jwt = jwtTokenProvider.resolveToken(httpServletRequest);
		String requestURI = httpServletRequest.getRequestURI();

		try {
			if (StringUtils.hasText(jwt)) {
				// 토큰 블랙리스트 확인
				if (tokenService.isTokenBlacklisted(jwt)) {
					log.debug("블랙리스트에 등록된 토큰입니다, uri: {}", requestURI);
				} else if (jwtTokenProvider.validateToken(jwt)) {
					// 토큰에서 인증 정보 추출하여 SecurityContext에 설정
					Authentication authentication = jwtTokenProvider.getAuthentication(jwt);
					SecurityContextHolder.getContext().setAuthentication(authentication);
					log.debug("Security Context에 '{}' 인증 정보를 저장했습니다, uri: {}",
							authentication.getName(), requestURI);
				} else {
					log.debug("유효하지 않은 JWT 토큰입니다, uri: {}", requestURI);
				}
			} else {
				log.debug("JWT 토큰이 없습니다, uri: {}", requestURI);
			}
		} catch (Exception e) {
			log.error("JWT 토큰 처리 중 오류 발생: {}", e.getMessage());
			SecurityContextHolder.clearContext();
		}

		filterChain.doFilter(servletRequest, servletResponse);
	}
}
