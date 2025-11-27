package com.roome.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Provider {
    LOCAL("Local"),
	GOOGLE("Google"),
	KAKAO("Kakao"),
	NAVER("Naver");

	private final String provider;
}
