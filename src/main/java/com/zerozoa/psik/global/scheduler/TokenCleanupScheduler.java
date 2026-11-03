package com.zerozoa.psik.global.scheduler;

import com.zerozoa.psik.repository.auth.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    // Cloud Scheduler가 매일 03시(UTC, KST 12시) /internal/jobs/cleanup-tokens로 호출한다 (InternalJobController)
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            // issuedAt은 발급·재발급 때마다 갱신되므로, 유효기간보다 오래전에 발급된 토큰만 만료된 것이다
            Instant cutoff = Instant.now().minusMillis(refreshTokenExpirationMs);
            refreshTokenRepository.deleteExpiredTokens(cutoff);
            log.info("[Scheduler] 만료 토큰 정리 완료 - cutoff={}", cutoff);
        } catch (Exception e) {
            log.error("[Scheduler] 만료 토큰 정리 실패", e);
        }
    }
}
