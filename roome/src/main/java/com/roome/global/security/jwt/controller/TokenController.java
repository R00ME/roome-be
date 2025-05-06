package com.roome.global.security.jwt.controller;

import com.roome.global.security.jwt.dto.GetAccessTokenByTempCodeRequest;
import com.roome.global.security.jwt.service.TempTokenService;
import com.roome.global.security.jwt.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/token")
public class TokenController {

	private final TempTokenService tempTokenService;
	private final TokenService tokenService;

	@PostMapping("/temp")
	public ResponseEntity<Void> exchangeTempCode(@RequestBody GetAccessTokenByTempCodeRequest getAccessTokenByTempCodeRequest,
												 HttpServletResponse response) {
		tempTokenService.exchangeTempCode(getAccessTokenByTempCodeRequest, response);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/refresh")
	public ResponseEntity<Void> refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
		tokenService.refreshAccessToken(request, response);
		return ResponseEntity.ok().build();
	}
}
