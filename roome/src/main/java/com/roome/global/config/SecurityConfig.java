package com.roome.global.config;

import com.roome.global.security.jwt.token.JwtTokenProvider;
import com.roome.global.security.oauth.client.KakaoAuthorizationCodeTokenResponseClient;
import com.roome.global.security.oauth.user.handler.OAuth2AuthenticationFailureHandler;
import com.roome.global.security.oauth.user.handler.OAuth2AuthenticationSuccessHandler;
import com.roome.global.security.oauth.user.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
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
        JwtSecurityConfig jwtSecurityConfig = new JwtSecurityConfig(jwtTokenProvider,blacklistRedisTemplate);

        httpSecurity
                // csrf 는 로그인 유저 올바른지 판단하기 위한 csrf 토큰 방식 -> rest api 구조 + JWT 사용으로 닫아놓음
                .csrf(csrf -> csrf.disable())
//                .exceptionHandling(exceptionHandling ->
//                        exceptionHandling
//                                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
//                                .accessDeniedHandler(jwtAccessDeniedHandler)
//                )
                .headers(headers ->
                        headers
                                .frameOptions(frameOptions -> frameOptions.sameOrigin())
                )
                .sessionManagement(sessionManagement ->
                        sessionManagement
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/api/authenticate").permitAll()
                                .requestMatchers("/api/users/signup").permitAll()
                                .requestMatchers("/callback", "/oauth2/**").permitAll()
                                .requestMatchers("/login").permitAll()
                                .requestMatchers("/auth/token/temp").permitAll()
                                .requestMatchers(PathRequest.toH2Console()).permitAll()
//                                .requestMatchers("/favicon.ico").permitAll()
                                .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .tokenEndpoint(token -> token
                                .accessTokenResponseClient(customAccessTokenResponseClient())
                        )
//                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                );

        jwtSecurityConfig.configure(httpSecurity); // JwtSecurityConfig를 적용

        return httpSecurity.build();
    }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> customAccessTokenResponseClient() {
        return request -> {
            String registrationId = request.getClientRegistration().getRegistrationId();

            if ("kakao".equals(registrationId)) {
                return new KakaoAuthorizationCodeTokenResponseClient().getTokenResponse(request);
            }

            // 기본 클라이언트: 구글, 네이버는 그대로
            return new DefaultAuthorizationCodeTokenResponseClient().getTokenResponse(request);
        };
    }
}
