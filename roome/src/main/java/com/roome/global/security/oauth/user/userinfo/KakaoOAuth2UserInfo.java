package com.roome.global.security.oauth.user.userinfo;

import com.roome.domain.user.entity.Provider;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

	private final Map<String, Object> attributes;

	@Override
	public Provider getProvider() {
		return Provider.KAKAO;
	}

	@Override
	public String getProviderId() {
		return String.valueOf(attributes.get("id"));
	}

	@Override
	public String getEmail() {
//        return (String) attributes.get("account_email");
		return String.valueOf(attributes.get("account_email"));
	}

	@Override
	public String getName() {
//        return (String) attributes.get("profile_nickname");
		return String.valueOf(attributes.get("profile_nickname"));
	}

	@Override
	public String getNickname() {
//        return (String) attributes.get("profile_nickname");
		return String.valueOf(attributes.get("profile_nickname"));
	}

	@Override
	public String getProfileImage() {
//        return (String) attributes.get("profile_image");
		return String.valueOf(attributes.get("profile_image"));
	}
}
