package com.roome.global.security.oauth.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class KakaoAuthorizationCodeTokenResponseClient implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest authorizationGrantRequest) {
        String tokenUri = authorizationGrantRequest.getClientRegistration().getProviderDetails().getTokenUri();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", authorizationGrantRequest.getClientRegistration().getClientId());
        params.add("redirect_uri", authorizationGrantRequest.getAuthorizationExchange().getAuthorizationRequest().getRedirectUri());
        params.add("code", authorizationGrantRequest.getAuthorizationExchange().getAuthorizationResponse().getCode());
        params.add("client_secret", authorizationGrantRequest.getClientRegistration().getClientSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        Map<String, Object> response = restTemplate.postForObject(tokenUri, request, Map.class);

        Object refreshTokenObj = response.get("refresh_token");
        String refreshToken = refreshTokenObj != null ? refreshTokenObj.toString() : null;

        System.out.println(response);

        return OAuth2AccessTokenResponse.withToken((String) response.get("access_token"))
                .tokenType(org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER)
                .expiresIn(((Number) response.get("expires_in")).longValue()) // expires_in이 Long일 수도 있어서 Number로 받음
                .refreshToken(refreshToken)
                .build();
    }
}
