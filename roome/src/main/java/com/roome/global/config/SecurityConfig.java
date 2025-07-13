package com.roome.global.config;

import com.roome.global.security.jwt.provider.JwtTokenProvider;
import com.roome.global.security.oauth.handler.OAuth2AuthenticationFailureHandler;
import com.roome.global.security.oauth.handler.OAuth2AuthenticationSuccessHandler;
import com.roome.global.security.oauth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtTokenProvider jwtTokenProvider;
	private final CustomOAuth2UserService customOAuth2UserService;
	private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
	private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
	@Qualifier("blacklistRedisTemplate")
	private final RedisTemplate<String, String> blacklistRedisTemplate;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() { // security를 적용하지 않을 리소스
		return web -> web.ignoring()
				// error endpoint를 열어줘야 함, favicon.ico 추가!
				.requestMatchers("/error", "/favicon.ico");
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
		JwtSecurityConfig jwtSecurityConfig = new JwtSecurityConfig(jwtTokenProvider, blacklistRedisTemplate);

		httpSecurity
				// csrf 는 로그인 유저 올바른지 판단하기 위한 csrf 토큰 방식 -> rest api 구조 + JWT 사용으로 닫아놓음
				.csrf(AbstractHttpConfigurer::disable)
//                .exceptionHandling(exceptionHandling ->
//                        exceptionHandling
//                                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
//                                .accessDeniedHandler(jwtAccessDeniedHandler)
//                )
				.headers(headers ->
						headers
								.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
				)
				.sessionManagement(sessionManagement ->
						sessionManagement
								.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
				)
				.authorizeHttpRequests(authorizeRequests ->
								authorizeRequests
										.requestMatchers("/ws/**").permitAll()
										.requestMatchers("/topic/**", "/app/**").permitAll()
										.requestMatchers("/api/authenticate").permitAll()
//										.requestMatchers("/api/users/signup").permitAll()
//										.requestMatchers("/callback", "/oauth2/**").permitAll()
										// .requestMatchers("/api/login/oauth2/code/**").permitAll()
										// .requestMatchers("/api/oauth2/**").permitAll()
						                                .requestMatchers("/login/oauth2/code/**").permitAll()
										.requestMatchers("/oauth2/**").permitAll()
//										.requestMatchers("/login").permitAll()
										.requestMatchers("/auth/token/temp").permitAll()
										.requestMatchers("/auth/token/refresh").permitAll()
										.requestMatchers("/api/auth/token/temp").permitAll()
										.requestMatchers("/api/auth/token/refresh").permitAll()
										.requestMatchers(PathRequest.toH2Console()).permitAll()
//                                .requestMatchers("/favicon.ico").permitAll()
										.anyRequest().authenticated()
				)
				.oauth2Login(oauth2 -> oauth2
						.userInfoEndpoint(userInfo -> userInfo
								.userService(customOAuth2UserService))
						.successHandler(oAuth2AuthenticationSuccessHandler)
						.failureHandler(oAuth2AuthenticationFailureHandler)
				);

		jwtSecurityConfig.configure(httpSecurity); // JwtSecurityConfig를 적용

		return httpSecurity.build();
	}
}
