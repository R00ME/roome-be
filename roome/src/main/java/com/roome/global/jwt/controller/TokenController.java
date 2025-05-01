package com.roome.global.jwt.controller;

import com.roome.global.jwt.token.JwtTokenProvider;
import com.roome.global.jwt.dto.GetAccessTokenByTempCodeRequest;
import com.roome.global.jwt.service.TempTokenService;
import com.roome.global.jwt.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/token")
public class TokenController {

    private final TempTokenService tempTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;

    @PostMapping("/temp")
    public ResponseEntity<Void> exchangeTempCode(@RequestBody GetAccessTokenByTempCodeRequest getAccessTokenByTempCodeRequest,
                                                 HttpServletResponse response) {
        String accessToken = tempTokenService.getAccessTokenByTempCode(getAccessTokenByTempCodeRequest.getTempCode());

        Long userId = jwtTokenProvider.getUserIdFromAccessToken(accessToken);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        // accessToken: Header로 전달
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        // refreshToken: Cookie로 전달
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofDays(14))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        tokenService.refreshAccessToken(request, response);
        return ResponseEntity.ok().build();
    }
}
