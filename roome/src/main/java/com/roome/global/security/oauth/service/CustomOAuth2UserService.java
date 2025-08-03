package com.roome.global.security.oauth.service;

import com.roome.domain.user.entity.Status;
import com.roome.domain.user.entity.User;
import com.roome.domain.user.entity.UserRole;
import com.roome.domain.user.repository.UserRepository;
import com.roome.global.security.oauth.factory.OAuth2UserInfoFactory;
import com.roome.global.security.oauth.model.CustomOAuth2User;
import com.roome.global.security.oauth.userinfo.OAuth2UserInfo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final UserRepository userRepository;

		private record UserResult(User user, boolean isNewUser) {}

	// OAuth2 서버 에서 사용자 정보 받는 메서드
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
		UserResult userResult = saveOrUpdate(userInfo);

		// 반환 -> 이후 principal 객체 역할
		return new CustomOAuth2User(userResult.user, attributes, userResult.user.getId(), userResult.isNewUser());
	}

	public UserResult saveOrUpdate(OAuth2UserInfo userInfo) {
		return userRepository.findByEmail(userInfo.getEmail())
				.map(user -> new UserResult(user, false))
				.orElseGet(() -> {
					User newUser = userRepository.save(User.builder()
							.email(userInfo.getEmail())
							.provider(userInfo.getProvider())
							.providerId(userInfo.getProviderId())
							.name(userInfo.getName() != null ? userInfo.getName() : "unknown") // name 기본값
							.nickname(userInfo.getNickname() != null ? userInfo.getNickname() : "nickname") // nickname 기본값
							.profileImage(userInfo.getProfileImage()) // (nullable 가능)
							.status(Status.ONLINE) // 상태 기본값 설정 -> 회원 가입 후 자동 로그인 -> ONLINE
							.userRole(UserRole.USER)
							.build());
					return new UserResult(newUser, true);
				});
	}
}
