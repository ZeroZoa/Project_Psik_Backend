package com.zerozoa.psik.global.security;

import com.zerozoa.psik.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GcpOidcVerifierTest {

    private static final String AUDIENCE = "https://psik-backend.example.run.app";
    private static final String SA = "pubsub-push@project.iam.gserviceaccount.com";

    @Test
    @DisplayName("Authorization 헤더가 없으면 거절한다")
    void noHeader_reject() {
        // given
        GcpOidcVerifier verifier = new GcpOidcVerifier(AUDIENCE, SA);

        // when & then
        assertThatThrownBy(() -> verifier.verify(null)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("서비스 계정 설정이 비어 있으면(local 등) 어떤 요청도 거절한다")
    void notConfigured_reject() {
        // given
        GcpOidcVerifier verifier = new GcpOidcVerifier("", "");

        // when & then
        assertThatThrownBy(() -> verifier.verify("Bearer anything")).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Google 서명이 없는 토큰은 거절한다")
    void forgedToken_reject() {
        // given
        GcpOidcVerifier verifier = new GcpOidcVerifier(AUDIENCE, SA);

        // when & then
        assertThatThrownBy(() -> verifier.verify("Bearer not.a.jwt")).isInstanceOf(BusinessException.class);
    }
}
