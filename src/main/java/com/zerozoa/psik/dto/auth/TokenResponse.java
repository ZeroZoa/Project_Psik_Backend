package com.zerozoa.psik.dto.auth;


public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
