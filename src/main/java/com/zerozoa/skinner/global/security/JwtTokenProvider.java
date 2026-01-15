package com.zerozoa.skinner.global.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            // 변경된 application.yaml 키 반영
            @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {

        // Base64로 인코딩된 키를 디코딩하여 사용 (보안 표준)
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);

        // 이미 ms 단위이므로 그대로 할당
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    // 1. Access Token 생성
    public String createAccessToken(String memberUuid, String role) {
        return createToken(memberUuid, role, accessTokenExpirationMs);
    }

    // 2. Refresh Token 생성
    public String createRefreshToken(String memberUuid) {
        return createToken(memberUuid, null, refreshTokenExpirationMs);
    }

    // 내부 토큰 생성 로직 (공통)
    private String createToken(String subject, String role, long expirationMs) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .subject(subject) // 사용자 식별자 (UUID)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key);

        // Role이 있는 경우에만 Claim에 추가 (Refresh Token은 Role 불필요)
        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    // 3. 토큰에서 Payload(Subject: UUID) 추출
    public String getPayload(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    // 4. 토큰 유효성 검사
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }
}
