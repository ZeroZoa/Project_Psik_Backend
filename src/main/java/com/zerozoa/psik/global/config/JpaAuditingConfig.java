package com.zerozoa.psik.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정
 * PsikApplication에서 분리하여 테스트 시 선택적으로 로드 가능
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
