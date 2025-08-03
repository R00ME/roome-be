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

@RequiredArgsConstructor
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private final JwtTokenProvider jwtTokenProvider;
	private final TokenExchangeService tokenExchangeService;
	@Value("${app.oauth2.redirectUri}")
	private String frontendRedirectUri ;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
										Authentication authentication) throws IOException {
		// Token 코드 발급
		String accessToken = jwtTokenProvider.createToken(authentication);

		// 임시 코드 발급 -> url 로 전달
		String tempCode = tokenExchangeService.generateTempCode(accessToken);

		// isNewUser 여부 추출
		CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
		boolean isNewUser = oAuth2User.isNewUser();

		// tempCode, isNewUser 함께 전달
		String redirectUrl = frontendRedirectUri + "/login/callback?temp_code=" + tempCode + "&is_new_user=" + isNewUser;
		response.sendRedirect(redirectUrl);
	}
}
