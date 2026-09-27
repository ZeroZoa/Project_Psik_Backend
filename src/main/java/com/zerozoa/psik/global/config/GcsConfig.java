package com.zerozoa.psik.global.config;

import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

/**
 * Google Cloud Storage Bean 설정
 * - @Profile("prod") — 로컬에서는 빈 생성 안 함
 * - Cloud Run Workload Identity → Application Default Credentials 자동 사용
 *   (별도 Service Account 키 파일 불필요)
 */
@Configuration
@Profile("prod")
public class GcsConfig {

    @Bean
    public Storage storage() {
        // 기본값은 타임아웃/재시도가 사실상 무제한이라, 네트워크 문제 시
        // SkinAnalysisWorker가 아무 로그도 없이 계속 멈춰있던 원인 중 하나였음.
        // 명시적으로 짧게 제한해서 이 단계에서 먼저 실패가 나게 함.
        RetrySettings retrySettings = RetrySettings.newBuilder()
                .setTotalTimeoutDuration(Duration.ofSeconds(15))
                .setInitialRpcTimeoutDuration(Duration.ofSeconds(8))
                .setMaxRpcTimeoutDuration(Duration.ofSeconds(8))
                .setInitialRetryDelayDuration(Duration.ofMillis(500))
                .setMaxRetryDelayDuration(Duration.ofSeconds(3))
                .setRetryDelayMultiplier(1.5)
                .setMaxAttempts(3)
                .build();

        return StorageOptions.newBuilder()
                .setRetrySettings(retrySettings)
                .build()
                .getService();
    }
}