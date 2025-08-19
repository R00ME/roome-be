package com.roome.global.security.oauth.handler;

import com.roome.global.security.jwt.service.TokenExchangeService;
import com.roome.global.security.jwt.provider.JwtTokenProvider;
import com.roome.global.security.oauth.model.CustomOAuth2User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenExchangeService tokenExchangeService;
    @Value("${app.oauth2.redirectUris}")
    private List<String> redirectUris;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String origin = request.getHeader("Origin");

        if (origin == null) {
            String referer = request.getHeader("Referer");
            if (referer != null) {
                // 마지막 '/' 제거
                if (referer.endsWith("/")) {
                    referer = referer.substring(0, referer.length() - 1);
                }
                origin = referer;
            }
        }

        String checkOrigin = origin;
        String targetUri = redirectUris.stream()
                .filter(uri -> checkOrigin != null && uri.startsWith(checkOrigin))
                .findFirst()
                .orElse("https://roome.io.kr"); // fallback

        String accessToken = jwtTokenProvider.createToken(authentication);
        String tempCode = tokenExchangeService.generateTempCode(accessToken);

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        boolean isNewUser = oAuth2User.isNewUser();

        String redirectUrl = targetUri + "/login/callback?temp_code=" + tempCode + "&is_new_user=" + isNewUser;
        response.sendRedirect(redirectUrl);
    }
}
