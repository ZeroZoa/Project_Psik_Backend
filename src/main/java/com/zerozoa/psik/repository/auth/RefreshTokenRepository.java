package com.zerozoa.psik.repository.auth;

import com.zerozoa.psik.domain.auth.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh Token 리포지토리
 * 토큰 재발급, 로그아웃, 만료 토큰 정리에 사용
 */

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    //토큰 값으로 조회(토큰 재발급 및 검증용)
    Optional<RefreshToken> findByToken(String token);

    //특정 회원의 모든 토큰 삭제(탈퇴 및 로그아웃용)
    void deleteAllByMemberUuid(UUID memberUuid);

    //만료된 refresh token 정리
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.issuedAt < :cutoff")
    void deleteExpiredTokens(Instant cutoff);
}