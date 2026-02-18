package com.zerozoa.skinner.domain.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {
    //데이터 생성 시간 (기준 : UTC)
    //updatable = false를 통해 최초 생성 후 수정 불가
    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    //데이터 마지막 수정 시간 (기준 : UTC)
    //데이터 변경이 일어날 때마다 자동으로 갱신
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    //*Spring Boot메인 클래스에 @EnableJpaAuditing을 추가해야 작동함*
}
