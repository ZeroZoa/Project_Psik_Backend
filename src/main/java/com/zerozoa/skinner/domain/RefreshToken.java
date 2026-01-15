package com.zerozoa.skinner.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RefreshToken {
    @Id
    private String memberUuid;

    private String token;

    private String ip;
    private String userAgent;

    @CreatedDate
    private Instant issuedAt;

    public RefreshToken(String memberUuid, String token, String ip, String userAgent) {
        this.memberUuid = memberUuid;
        this.token = token;
        this.ip = ip;
        this.userAgent = userAgent;
    }

    public void updateToken(String newToken, String ip, String userAgent) {
        this.token = newToken;
        this.ip = ip;
        this.userAgent = userAgent;
        this.issuedAt = Instant.now();
    }
}
