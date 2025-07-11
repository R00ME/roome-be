package com.roome.global.security.oauth.userinfo;

import com.roome.domain.user.entity.Provider;

public interface OAuth2UserInfo {
	Provider getProvider();

	String getProviderId();

	String getEmail();

	String getName();

	String getNickname();

	String getProfileImage();
}
