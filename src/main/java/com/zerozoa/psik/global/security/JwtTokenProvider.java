package com.zerozoa.psik.global.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_ROLE = "role";
    public static final String TYPE_ACCESS = "ACCESS";
    public static final String TYPE_REFRESH = "REFRESH";

    private final SecretKey key;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {

        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    //Access Token 생성 (UUID 입력)
    public String createAccessToken(UUID memberUuid, String role) {
        return createToken(memberUuid.toString(), role, TYPE_ACCESS, accessTokenExpirationMs);
    }

    //Refresh Token 생성 (UUID 입력)
    public String createRefreshToken(UUID memberUuid) {
        return createToken(memberUuid.toString(), null, TYPE_REFRESH, refreshTokenExpirationMs);
    }

    //내부 토큰 생성 로직 — typ 클레임으로 Access/Refresh를 구분해 서로 바꿔 쓸 수 없게 한다
    private String createToken(String subject, String role, String type, long expirationMs) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .subject(subject) // UUID String이 들어감
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key);

        if (role != null) {
            builder.claim(CLAIM_ROLE, role);
        }

        return builder.compact();
    }

    //Payload 추출 — 필터/서비스가 한 번만 파싱해서 subject/role/typ을 같이 꺼내 쓰도록 공개
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    //토큰에서 UUID 추출 (String으로 반환하되, 호출부에서 변환)
    public String getPayload(String token) {
        try {
            return parseClaims(token).getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
    }

    //토큰에서 Role 추출
    public String getRole(String token) {
        try {
            return parseClaims(token).get(CLAIM_ROLE, String.class);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    //토큰 종류(ACCESS/REFRESH) 추출
    public String getTokenType(String token) {
        try {
            return parseClaims(token).get(CLAIM_TYPE, String.class);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    //Refresh Token인지 확인 (reissue 요청에 Access Token이 들어오는 것을 차단할 때 사용)
    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(getTokenType(token));
    }

    //토큰 유효성 검사
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.");
            throw e; // Filter에서 잡아 401 반환
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("잘못된 JWT 서명입니다.");
            throw new JwtException("잘못된 JWT 서명", e);
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다.");
            throw new JwtException("지원되지 않는 JWT", e);
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 잘못되었습니다.");
            throw new JwtException("JWT 토큰이 잘못됨", e);
        }
    }
}
