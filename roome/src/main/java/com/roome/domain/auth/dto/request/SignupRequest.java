package com.roome.domain.auth.dto.request;

public record SignupRequest(
        String email,
        String password,
        String gender,
        String birthDate,
        String nickname,
        String bio
) {}
