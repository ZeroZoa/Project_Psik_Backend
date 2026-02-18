package com.zerozoa.skinner.repository.auth;

import com.zerozoa.skinner.domain.auth.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    //토큰 값으로 조회(토큰 재발급 및 검증용)
    Optional<RefreshToken> findByToken(String token);

    //특정 회원의 모든 토큰 삭제(탈퇴 및 로그아웃용)
    void deleteAllByMemberUuid(UUID memberUuid);
}