package com.zerozoa.psik.global.config;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

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
        return StorageOptions.getDefaultInstance().getService();
    }
}