package com.roome.global.config;

import com.roome.global.security.filter.JwtAuthenticationFilter;
import com.roome.global.security.jwt.provider.JwtTokenProvider;
import com.roome.global.security.jwt.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@RequiredArgsConstructor
@Configuration
public class JwtSecurityConfig extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

	private final JwtTokenProvider jwtTokenProvider;
	private final TokenService tokenService;

	@Override
	public void configure(HttpSecurity http) {
		// 필터 동작 전 직접 구성한 JwtFilter 주입
		http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, tokenService),
				UsernamePasswordAuthenticationFilter.class);
	}
}
