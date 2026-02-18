package com.zerozoa.skinner.domain.auth;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

//Refresh Token 엔티티
//Access Token 만료 후 재발급을 위한 인증수단
@Entity
@Getter
@Table(name = "refresh_tokens", indexes = {
        //토큰 값으로 조회
        @Index(name = "idx_refresh_token_value", columnList = "token"), // 토큰 값으로 검색 속도 향상
        //특정 회원 기기 전체 로그아웃을 취한 식별자로 조회
        @Index(name = "idx_refresh_token_member", columnList = "member_uuid") // 특정 유저의 토큰 전체 삭제 시 사용
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // [수정] PK를 별도로 두어 1인 다중 기기 로그인 지원

    @Column(name = "member_uuid", columnDefinition = "uuid", nullable = false)
    private UUID memberUuid;

    @Column(nullable = false, length = 1000) // 토큰 값 자체는 유니크할 필요 없음 (재발급 시 덮어쓰거나 삭제하므로)
    private String token;

    private String ip;

    private String userAgent;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    public RefreshToken(UUID memberUuid, String token, String ip, String userAgent) {
        this.memberUuid = memberUuid;
        this.token = token;
        this.ip = ip;
        this.userAgent = userAgent;
        this.issuedAt = Instant.now();
    }

    // 토큰 교체 (RTR: Refresh Token Rotation)
    public void rotateToken(String newToken, String ip, String userAgent) {
        this.token = newToken;
        this.ip = ip;
        this.userAgent = userAgent;
        this.issuedAt = Instant.now();
    }
}