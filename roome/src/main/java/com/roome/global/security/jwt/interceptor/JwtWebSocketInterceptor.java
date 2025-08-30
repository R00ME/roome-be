package com.roome.global.security.jwt.interceptor;

import com.roome.global.exception.BusinessException;
import com.roome.global.exception.ErrorCode;
import com.roome.global.security.jwt.principal.CustomUser;
import com.roome.global.security.jwt.provider.JwtTokenProvider;
import com.roome.global.security.jwt.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class JwtWebSocketInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 웹소켓 연결 요청일 때만 인증 처리
            String token = extractTokenFromHeader(accessor);

            if (token != null) {

                // tempCode인지 확인
                if (isTempCode(token)) {
                    log.warn("[WebSocket 연결 거부] tempCode로는 웹소켓 연결 불가");
                    throw new BusinessException(ErrorCode.WEBSOCKET_TEMP_CODE_NOT_ALLOWED);
                }

                // 블랙리스트 체크
                if (tokenService.isTokenBlacklisted(token)) {
                    log.warn("[WebSocket 연결 거부] 블랙리스트에 등록된 토큰");
                    throw new BusinessException(ErrorCode.WEBSOCKET_TOKEN_BLACKLISTED);
                }

                // 토큰 검증
                if (jwtTokenProvider.validateToken(token)) {
                    Long userId = jwtTokenProvider.getUserIdFromAccessToken(token);
                    String email = jwtTokenProvider.getEmailFromAccessToken(token);         // 🔧 추가
                    List<SimpleGrantedAuthority> authorities = jwtTokenProvider.getAuthorities(token).stream() // 🔧 추가
                            .map(SimpleGrantedAuthority::new)
                            .toList();

                    try {

                        // SecurityContext에 인증 정보 설정 (JwtAuthenticationFilter와 유사)
                        CustomUser customUser = new CustomUser(userId, email, authorities);
                        Authentication auth = new UsernamePasswordAuthenticationToken(customUser, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);

                        // accessor가 mutable인 경우에만 User 설정
                        if (accessor.isMutable()) {
                            accessor.setUser(auth);
                        }

                        log.info("[WebSocket 연결 성공] 사용자 ID: {}", userId);
                    } catch (NumberFormatException e) {
                        log.warn("[WebSocket 연결 거부] 토큰의 userId 형식이 올바르지 않음");
                        throw new BusinessException(ErrorCode.INVALID_USER_ID_FORMAT);
                    }
                } else {
                    log.warn("[WebSocket 연결 거부] 유효하지 않은 토큰");
                    throw new BusinessException(ErrorCode.WEBSOCKET_TOKEN_INVALID);
                }
            } else {
                log.warn("[WebSocket 연결 거부] 토큰 없음");
                throw new BusinessException(ErrorCode.WEBSOCKET_TOKEN_MISSING);
            }
        }

        return message;
    }

    private String extractTokenFromHeader(StompHeaderAccessor accessor) {
        // STOMP 헤더에서 JWT 토큰 추출
        String bearerToken = null;

        if (accessor.getNativeHeader("Authorization") != null && !accessor.getNativeHeader("Authorization").isEmpty()) {
            bearerToken = accessor.getNativeHeader("Authorization").get(0);
        }

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    private boolean isTempCode(String token) {
        // UUID 형식인지 확인 (tempCode는 UUID, JWT는 다른 형식)
        try {
            UUID.fromString(token);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
