package com.roome.global.oauth.user.factory;

import com.roome.global.oauth.user.userinfo.GoogleOAuth2UserInfo;
import com.roome.global.oauth.user.userinfo.KakaoOAuth2UserInfo;
import com.roome.global.oauth.user.userinfo.OAuth2UserInfo;

import java.util.Map;

public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(String provider, Map<String, Object> attributes) {
        return switch (provider) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "kakao" -> new KakaoOAuth2UserInfo(attributes);
//            case "naver" -> new NaverOAuth2UserInfo(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.");
        };
    }
}
