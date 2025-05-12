package com.roome.global.security.oauth.factory;

import com.roome.global.exception.ControllerException;
import com.roome.global.exception.ErrorCode;
import com.roome.global.security.oauth.userinfo.GoogleOAuth2UserInfo;
import com.roome.global.security.oauth.userinfo.KakaoOAuth2UserInfo;
import com.roome.global.security.oauth.userinfo.NaverOAuth2UserInfo;
import com.roome.global.security.oauth.userinfo.OAuth2UserInfo;

import java.util.Map;

public class OAuth2UserInfoFactory {
	public static OAuth2UserInfo getOAuth2UserInfo(String provider, Map<String, Object> attributes) {
		return switch (provider) {
			case "google" -> new GoogleOAuth2UserInfo(attributes);
			case "kakao" -> new KakaoOAuth2UserInfo(attributes);
			case "naver" -> new NaverOAuth2UserInfo(attributes);
			default -> throw new ControllerException(ErrorCode.INVALID_LOGIN);
		};
	}
}
