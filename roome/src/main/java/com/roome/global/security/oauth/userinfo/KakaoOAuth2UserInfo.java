package com.roome.global.security.oauth.userinfo;

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
		return (String) ((Map<String, Object>) attributes.get("kakao_account")).get("email");
	}

	@Override
	public String getName() {
		return (String) ((Map<String, Object>) ((Map<String, Object>) attributes.get("kakao_account"))
				.get("profile")).get("nickname");
	}

	@Override
	public String getNickname() {
		return getName();
	}

	@Override
	public String getProfileImage() {
		Map<String, Object> profile = (Map<String, Object>) ((Map<String, Object>) attributes.get("kakao_account")).get("profile");
		return profile != null ? (String) profile.get("profile_image_url") : null;
	}
}
