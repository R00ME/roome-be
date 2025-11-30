package com.roome.domain.auth.service;

import com.roome.domain.auth.dto.request.LoginRequest;
import com.roome.domain.auth.dto.request.SignupRequest;
import com.roome.domain.user.entity.Provider;
import com.roome.domain.user.entity.Status;
import com.roome.domain.user.entity.User;
import com.roome.domain.user.entity.UserRole;
import com.roome.domain.user.repository.UserRepository;
import com.roome.global.exception.BusinessException;
import com.roome.global.exception.ErrorCode;
import com.roome.global.security.jwt.provider.JwtTokenProvider;
import com.roome.global.security.jwt.service.RefreshTokenService;
import com.roome.global.security.jwt.service.TokenService;
import com.roome.global.security.jwt.util.TokenResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final JwtTokenProvider jwtTokenProvider;

	@Qualifier("blacklistRedisTemplate")
	private final RedisTemplate<String, Long> blacklistRedisTemplate;
	private final RefreshTokenService refreshTokenService;
	private final TokenService tokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User signup(SignupRequest signupRequest) {
        // 이미 가입된 이메일인지 확인
        Optional<User> existing = userRepository.findByEmail(signupRequest.email());

        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getProvider() != Provider.LOCAL) {
                throw new RuntimeException("해당 이메일은 소셜 계정입니다.");
            }
            throw new RuntimeException("이미 가입된 이메일입니다.");
        }

        // 비밀번호 확인
        if(!signupRequest.password().equals(signupRequest.confirmPassword())){
            throw new BusinessException(ErrorCode.PASSWORD_DISMATCH);
        }
        // 비밀번호 암호화
        String encodedPw = passwordEncoder.encode(signupRequest.password());

        // User 생성
        User user = User.builder()
                .email(signupRequest.email())
                .password(encodedPw)
                .name(signupRequest.nickname())
                .provider(Provider.LOCAL)
                .providerId(null)
                .userRole(UserRole.USER)
                .status(Status.ONLINE)
                .nickname(signupRequest.nickname())
                .build();

        return userRepository.save(user);
    }

    public void login(LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response) {

        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(RuntimeException::new);

        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new RuntimeException();
        }

        Authentication auth = jwtTokenProvider.getAuthenticationFromUserId(user.getId());

        String accessToken = jwtTokenProvider.createToken(auth);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        refreshTokenService.saveRefreshToken(user.getId(), refreshToken, request);

        TokenResponseUtil.addTokensToResponse(response, accessToken, refreshToken);
    }

	public void logout(Long userId, HttpServletRequest request, HttpServletResponse response) {

		String accessToken = jwtTokenProvider.resolveToken(request);
		if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {

			// 1. RefreshToken 삭제
			refreshTokenService.deleteRefreshToken(userId);

			// 2. AccessToken 블랙리스트 등록
			tokenService.addAccessTokenToBlacklist(accessToken);
		}

		// 쿠키 refreshToken 삭제
		ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
				.httpOnly(true)
				.secure(true)
				.sameSite("None")
				.path("/")
				.maxAge(0)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
	}
}
