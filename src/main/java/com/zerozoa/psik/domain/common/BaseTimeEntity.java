package com.zerozoa.psik.domain.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;


/**
 * 생성·수정 시각을 자동 관리하는 공통 기반 클래스
 * Spring Data JPA Auditing을 사용하며, 메인 클래스에 @EnableJpaAuditing 필수
 * 모든 시각은 UTC 기준 Instant로 저장 (타임존 이슈 방지)
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    /** 데이터 생성 시각 (UTC) — 최초 저장 후 변경 불가 */
    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    /** 데이터 마지막 수정 시각 (UTC) — 변경 발생 시 자동 갱신 */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
