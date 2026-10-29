package com.zerozoa.psik.global.scheduler;

import com.zerozoa.psik.repository.auth.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenCleanupSchedulerTest {

    private static final long REFRESH_EXPIRATION_MS = Duration.ofDays(14).toMillis();

    @Mock RefreshTokenRepository refreshTokenRepository;
    @InjectMocks TokenCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "refreshTokenExpirationMs", REFRESH_EXPIRATION_MS);
    }

    @Test
    @DisplayName("cleanupExpiredTokens: 기준 시각은 현재로부터 Refresh Token 유효기간 전이다 (유효한 토큰은 지우지 않음)")
    void cleanup_usesExpirationCutoff() {
        // given
        Instant before = Instant.now();

        // when
        scheduler.cleanupExpiredTokens();

        // then
        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(refreshTokenRepository).deleteExpiredTokens(captor.capture());
        Instant expected = before.minusMillis(REFRESH_EXPIRATION_MS);
        assertThat(captor.getValue()).isBetween(expected, expected.plusSeconds(5));
    }

    @Test
    @DisplayName("cleanupExpiredTokens: 삭제가 실패해도 예외를 밖으로 던지지 않는다 (스케줄러 유지)")
    void cleanup_deleteFails_noThrow() {
        // given
        doThrow(new RuntimeException("db down")).when(refreshTokenRepository).deleteExpiredTokens(any());

        // when & then
        assertThatCode(() -> scheduler.cleanupExpiredTokens()).doesNotThrowAnyException();
    }
}
