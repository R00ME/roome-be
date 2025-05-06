package com.roome.global.oauth.user.service;

import com.roome.domain.user.entity.Status;
import com.roome.domain.user.entity.User;
import com.roome.domain.user.entity.UserRole;
import com.roome.domain.user.repository.UserRepository;
import com.roome.global.oauth.user.factory.OAuth2UserInfoFactory;
import com.roome.global.oauth.user.model.CustomOAuth2User;
import com.roome.global.oauth.user.userinfo.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // provider 정보 가져오기
        String provider = userRequest.getClientRegistration().getRegistrationId();

        // 사용자 정보 attributes 가져오기(소설 정보)
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // provider 별 파싱
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(provider, attributes);

        // db 조회 / 회원가입 처리
        User user = saveOrUpdate(userInfo);

        // 반환 -> 이후 principal 객체 역할
        return new CustomOAuth2User(user, attributes, user.getId());
    }

    private User saveOrUpdate(OAuth2UserInfo userInfo) {
        return userRepository.findByEmail(userInfo.getEmail())
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(userInfo.getEmail())
                        .provider(userInfo.getProvider())
                        .providerId(userInfo.getProviderId())
                        .name(userInfo.getName() != null ? userInfo.getName() : "unknown") // name 기본값
                        .nickname(userInfo.getNickname() != null ? userInfo.getNickname() : "nickname") // nickname 기본값
                        .profileImage(userInfo.getProfileImage()) // (nullable 가능)
                        .status(Status.ONLINE) // 상태 기본값 설정 -> 회원 가입 후 자동 로그인 -> ONLINE
                        .userRole(UserRole.USER)
                        .build()));
    }
}
