package com.roome.global.oauth.user.handler;

import com.roome.global.jwt.token.JwtTokenProvider;
import com.roome.global.jwt.service.RefreshTokenService;
import com.roome.global.jwt.service.TempTokenService;
import com.roome.global.oauth.user.model.CustomOAuth2User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final TempTokenService tempTokenService;
    private final RefreshTokenService refreshTokenService;

    @Qualifier("refreshTokenRedisTemplate")
    private final RedisTemplate<String, String> refreshTokenRedisTemplate;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        Long userId = ((CustomOAuth2User) authentication.getPrincipal()).getId();

        // accessToken 코드 발급
        String accessToken = jwtTokenProvider.createToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        // 임시 코드 발급 -> url 로 전달
        String tempCode = tempTokenService.generateTempCode(accessToken);

        refreshTokenService.saveRefreshToken(userId, refreshToken);

        String redirectUrl = "http://localhost:8080/callback?temp_code=" + tempCode;
        System.out.println("🔁 Redirecting to: " + redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}
