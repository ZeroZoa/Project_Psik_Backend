package com.zerozoa.skinner.dto.auth;

import lombok.Builder;

@Builder
public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
