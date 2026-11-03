package com.zerozoa.psik.global.security;

import com.google.api.client.json.webtoken.JsonWebToken;
import com.google.auth.oauth2.TokenVerifier;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Pub/Sub push·Cloud Scheduler가 보내는 Google OIDC 토큰 검증.
 * Google 서명 + audience(이 서비스 URL) + 지정한 서비스 계정 이메일이 모두 맞아야 통과한다.
 */
@Slf4j
@Component
public class GcpOidcVerifier {

    private final TokenVerifier verifier;
    private final String allowedEmail;

    public GcpOidcVerifier(@Value("${gcp.oidc.audience:}") String audience,
                           @Value("${gcp.oidc.service-account:}") String allowedEmail) {
        this.verifier = TokenVerifier.newBuilder().setAudience(audience).build();
        this.allowedEmail = allowedEmail;
    }

    public void verify(String authorization) {
        if (allowedEmail.isBlank() || authorization == null || !authorization.startsWith("Bearer ")) {
            throw invalid();
        }
        try {
            JsonWebToken.Payload payload = verifier.verify(authorization.substring(7)).getPayload();
            if (!allowedEmail.equals(payload.get("email")) || !Boolean.TRUE.equals(payload.get("email_verified"))) {
                log.warn("[OIDC] 허용되지 않은 서비스 계정 - email={}", payload.get("email"));
                throw invalid();
            }
        } catch (TokenVerifier.VerificationException | IllegalArgumentException e) {   // 형식이 깨진 토큰은 IllegalArgumentException
            log.warn("[OIDC] 토큰 검증 실패 - {}", e.getMessage());
            throw invalid();
        }
    }

    private BusinessException invalid() {
        return new BusinessException(ErrorCode.INVALID_TOKEN, "유효하지 않은 토큰입니다.");
    }
}
