package com.zerozoa.psik.global.scheduler;

import com.zerozoa.psik.repository.auth.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {
    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")  // 매일 새벽 3시
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(Instant.now());
        //log.info("[Scheduler] 만료 토큰 정리 완료")
    }
}