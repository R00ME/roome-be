package com.roome.domain.auth.controller;

import com.roome.domain.auth.dto.request.LoginRequest;
import com.roome.domain.auth.dto.request.SignupRequest;
import com.roome.domain.auth.dto.response.LoginResponse;
import com.roome.domain.auth.dto.response.MessageResponse;
import com.roome.domain.auth.service.AuthService;
import com.roome.domain.furniture.entity.Furniture;
import com.roome.domain.furniture.entity.FurnitureType;
import com.roome.domain.furniture.repository.FurnitureRepository;
import com.roome.domain.room.dto.RoomResponseDto;
import com.roome.domain.room.service.RoomService;
import com.roome.domain.user.entity.User;
import com.roome.domain.user.repository.UserRepository;
import com.roome.domain.user.service.UserService;
import com.roome.global.exception.BusinessException;
import com.roome.global.security.jwt.exception.InvalidJwtTokenException;
import com.roome.global.security.jwt.exception.InvalidUserIdFormatException;
import com.roome.global.security.jwt.exception.MissingUserIdFromTokenException;
import com.roome.global.security.jwt.principal.CustomUser;
import com.roome.global.security.jwt.provider.JwtTokenProvider;
import com.roome.global.security.jwt.service.RefreshTokenService;
import com.roome.global.security.jwt.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "인증/인가 API", description = "인증/인가 관련 API")
public class AuthController {

	private final UserRepository userRepository;
	private final RoomService roomService;
	private final FurnitureRepository furnitureRepository;
	private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final TokenService tokenService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody SignupRequest signupRequest){
        User newUser = authService.signup(signupRequest);
        URI location = URI.create("/api/users/" + newUser.getId());
        return  ResponseEntity.created(location).build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        authService.login(loginRequest, request, response);
        return ResponseEntity.ok().build();
    }

	@Operation(summary = "사용자 정보 조회", description = "Access Token으로 사용자 정보를 조회합니다.", security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
			@ApiResponse(responseCode = "401", description = "인증 실패 또는 유효하지 않은 토큰")})
	@GetMapping("/user")
	public ResponseEntity<LoginResponse> getUserInfo(
			@AuthenticationPrincipal CustomUser customUser,
			HttpServletRequest httpServletRequest
	) {
			try {
//			String accessToken = httpServletRequest.getHeader("Authorization").substring(7);
			if (customUser == null) {
    			    throw new RuntimeException("CustomUser is null");
			}
			Long userId = customUser.getId();
			log.info("🧍 user = {}", customUser);
			
			User user = userRepository.findById(customUser.getId())
					.orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
			RoomResponseDto roomInfo = roomService.getOrCreateRoomByUserId(userId);

			// 가구 레벨 정보 조회
			Integer bookshelfLevel = 1;
			Integer cdRackLevel = 1;

			// 사용자의 방에 있는 가구 정보 조회
			List<Furniture> furnitures = furnitureRepository.findByRoomId(roomInfo.getRoomId());
			for (Furniture furniture : furnitures) {
				if (furniture.getFurnitureType() == FurnitureType.BOOKSHELF) {
					bookshelfLevel = furniture.getLevel();
				} else if (furniture.getFurnitureType() == FurnitureType.CD_RACK) {
					cdRackLevel = furniture.getLevel();
				}
			}

//			String refreshToken = refreshTokenService.getStoredFingerprint(userId).get("token");
//			log.info("User ID: {}, Refresh Token: {}", userId, refreshToken);

			LoginResponse loginResponse = LoginResponse.builder()
					.user(LoginResponse.UserInfo.builder()
							.userId(user.getId())
							.nickname(user.getNickname())
							.email(user.getEmail())
							.roomId(roomInfo.getRoomId())
							.profileImage(user.getProfileImage())
							.bookshelfLevel(bookshelfLevel)
							.cdRackLevel(cdRackLevel)
							.build())
					.build();

			return ResponseEntity.ok(loginResponse);
		} catch (Exception e) {
			log.error("사용자 정보 조회 중 오류: ", e);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
		}
	}

	// logout
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUser user,
									   HttpServletRequest request, HttpServletResponse response
	) {
		authService.logout(user.getId(), request, response);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "회원 탈퇴", description = "현재 로그인된 사용자 계정을 삭제합니다.", security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청"),
			@ApiResponse(responseCode = "401", description = "인증 실패 또는 유효하지 않은 토큰"),
			@ApiResponse(responseCode = "500", description = "서버 내부 오류")})
	@DeleteMapping("/withdraw")
	public ResponseEntity<MessageResponse> withdraw(
            @AuthenticationPrincipal CustomUser customUser,
			@RequestHeader("Authorization") String authHeader) {

		// 1. 토큰 파싱 및 검증
		String accessToken;
		Long userId = customUser.getId();

		try {
			if (authHeader == null || !authHeader.startsWith("Bearer ")) {
				log.warn("[회원탈퇴] 유효하지 않은 인증 헤더 형식");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new MessageResponse("유효한 인증 토큰이 필요합니다."));
			}

			accessToken = authHeader.substring(7);

			if (accessToken.isBlank()) {
				log.warn("[회원탈퇴] 빈 액세스 토큰");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new MessageResponse("유효한 액세스 토큰이 필요합니다."));
			}

			// 액세스 토큰 검증
            if (!jwtTokenProvider.validateToken(accessToken)) {
                log.warn("[회원탈퇴] 유효하지 않은 액세스 토큰: {}", maskToken(accessToken));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new MessageResponse("유효하지 않은 액세스 토큰입니다."));
            }

			// 유저 ID 추출
			try {
                userId = jwtTokenProvider.getUserIdFromAccessToken(accessToken);
				log.info("[회원탈퇴] 사용자 ID: {} 탈퇴 시작", userId);
			} catch (InvalidJwtTokenException | InvalidUserIdFormatException |
                     MissingUserIdFromTokenException e) {
				log.warn("[회원탈퇴] 토큰에서 사용자 ID 추출 실패: {}", e.getMessage());
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new MessageResponse("토큰에서 사용자 정보를 추출할 수 없습니다: " + e.getMessage()));
			}

		} catch (Exception e) {
			log.error("[회원탈퇴] 토큰 처리 중 예상치 못한 오류: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new MessageResponse("인증 처리 중 오류가 발생했습니다."));
		}

		// 2. Redis 작업 - 토큰 관련 처리
		try {
			// Refresh Token 삭제
            refreshTokenService.deleteRefreshToken(userId);
			log.debug("[회원탈퇴] 리프레시 토큰 삭제 성공: userId={}", userId);

			// Access Token 블랙리스트 추가
			long remainingTime = jwtTokenProvider.getRemainingValidity(accessToken);

			if (remainingTime > 0) {
				tokenService.addAccessTokenToBlacklist(accessToken);
				log.debug("[회원탈퇴] 액세스 토큰 블랙리스트 추가 성공: userId={}, 남은시간={}ms", userId, remainingTime);
			}
		} catch (Exception e) {
			// Redis 작업 실패는 기록하고 계속 진행
			log.warn("[회원탈퇴] Redis 작업 실패 (계속 진행): userId={}, 사유={}", userId, e.getMessage());
		}

		// 3. DB 작업 (사용자 데이터 삭제) - 트랜잭션 경계를 서비스 메서드 내로 이동
		try {
			userService.deleteUser(userId);
			log.info("[회원탈퇴] DB 작업 성공: userId={}", userId);
		} catch (BusinessException e) {
			log.error("[회원탈퇴] 비즈니스 예외: 코드={}, 메시지={}", e.getErrorCode(), e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new MessageResponse(e.getMessage()));
		} catch (Exception e) {
			log.error("[회원탈퇴] DB 작업 실패: userId={}, 사유={}", userId, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new MessageResponse("회원 탈퇴 처리 중 오류가 발생했습니다: " + e.getMessage()));
		}

		return ResponseEntity.ok(new MessageResponse("회원 탈퇴가 완료되었습니다."));
	}

	// 토큰 마스킹 (로그 보안)
	private String maskToken(String token) {
		if (token == null || token.length() < 10) {
			return "[too short to mask]";
		}
		return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
	}
}
