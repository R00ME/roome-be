package com.roome.global.oauth.user.handler;

import com.roome.global.jwt.service.RefreshTokenService;
import com.roome.global.jwt.service.TempTokenService;
import com.roome.global.jwt.token.JwtTokenProvider;
import com.roome.global.oauth.user.model.CustomOAuth2User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        Long userId = ((CustomOAuth2User) authentication.getPrincipal()).getId();

        // Token 코드 발급
        String accessToken = jwtTokenProvider.createToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        // 임시 코드 발급 -> url 로 전달
        String tempCode = tempTokenService.generateTempCode(accessToken);

        // refreshToken redis 에 저장
        refreshTokenService.saveRefreshToken(userId, refreshToken);

        //redirectUrl 로 tempCode 반환
        String redirectUrl = "http://localhost:8080/callback?temp_code=" + tempCode;
        System.out.println("🔁 Redirecting to: " + redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}
