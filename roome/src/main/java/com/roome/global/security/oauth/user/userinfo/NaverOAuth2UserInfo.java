package com.roome.global.security.oauth.user.userinfo;

import com.roome.domain.user.entity.Provider;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class NaverOAuth2UserInfo implements OAuth2UserInfo{

    private final Map<String, Object> attributes;
    @Override
    public Provider getProvider() {
        return Provider.NAVER;
    }

    @Override
    public String getProviderId() {
        return (String) ((Map) attributes.get("response")).get("id");
    }

    @Override
    public String getEmail() {
        return (String) ((Map) attributes.get("response")).get("email");
    }

    @Override
    public String getName() {
        return (String) ((Map) attributes.get("response")).get("nickname");
    }

    @Override
    public String getNickname() {
        return (String) ((Map) attributes.get("response")).get("nickname");
    }

    @Override
    public String getProfileImage() {
        return (String) ((Map) attributes.get("response")).get("profile_image");
    }
}
