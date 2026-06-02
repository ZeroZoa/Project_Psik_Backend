package com.zerozoa.psik.domain.auth;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh Token 엔티티
 * Access Token 만료 후 재발급을 위한 인증 수단
 * 1인 다중 기기 로그인을 지원하기 위해 PK(id)를 별도로 두어 기기별 토큰을 독립 관리
 * RTR(Refresh Token Rotation) 방식 적용 - 재발급 시 기존 토큰을 새 토큰으로 교체
 */
@Entity
@Getter
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token_value", columnList = "token"),      // 토큰 값으로 빠른 조회
        @Index(name = "idx_refresh_token_member", columnList = "member_uuid") // 특정 회원의 토큰 전체 삭제 시 사용
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    /** 내부 PK - 다중 기기 로그인 지원을 위해 별도 식별자로 관리 */
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 외부 PK - 토큰 소유 회원의 UUID*/
    @Column(name = "member_uuid", columnDefinition = "uuid", nullable = false)
    private UUID memberUuid;

    /** Refresh Token 값 (재발급 시 교체되므로 unique 제약 없음) */
    @Column(nullable = false, length = 1000)
    private String token;

    /** 토큰 발급 시점의 클라이언트 IP */
    private String ip;

    /** 토큰 발급 시점의 User-Agent (기기 식별용) */
    private String userAgent;

    /** 토큰 발급(또는 마지막 갱신) 시각 - 만료 토큰 정리 스케줄러에서 사용 */
    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    public RefreshToken(UUID memberUuid, String token, String ip, String userAgent) {
        this.memberUuid = memberUuid;
        this.token = token;
        this.ip = ip;
        this.userAgent = userAgent;
        this.issuedAt = Instant.now();
    }

    /**
     * 토큰 교체 (RTR: Refresh Token Rotation)
     * 재발급 요청 시 호출하여 기존 토큰을 새 토큰으로 교체하고 발급 시각을 갱신
     */
    public void rotateToken(String newToken, String ip, String userAgent) {
        this.token = newToken;
        this.ip = ip;
        this.userAgent = userAgent;
        this.issuedAt = Instant.now();
    }
}