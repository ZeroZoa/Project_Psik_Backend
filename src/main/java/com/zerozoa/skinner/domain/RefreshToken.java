package com.zerozoa.skinner.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {
    @Id
    @Column(name = "member_uuid", columnDefinition = "uuid")
    private UUID memberUuid;

    @Column(nullable = false, length = 1000)
    private String token;

    private String ip;

    private String userAgent;

    @Column(name = "issued_at", nullable = false) //@CreateDate대신 정확도를 위해 직접 조작
    private Instant issuedAt;

    public RefreshToken(UUID memberUuid, String token, String ip, String userAgent) {
        this.memberUuid = memberUuid;
        this.token = token;
        this.ip = ip;
        this.userAgent = userAgent;
        this.issuedAt = Instant.now();
    }

    public void updateToken(String newToken, String ip, String userAgent) {
        this.token = newToken;
        this.ip = ip;
        this.userAgent = userAgent;
        this.issuedAt = Instant.now();
    }
}
