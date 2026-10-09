package com.zerozoa.psik.repository.member;

import com.zerozoa.psik.domain.member.Member;
import com.zerozoa.psik.domain.member.Provider;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 회원 Repository
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    // 소셜 로그인 시 회원 조회 — (provider, oauthId) 복합 인덱스 사용
    Optional<Member> findByProviderAndOauthId(Provider provider, String oauthId);

    // Access Token 검증 후 회원 조회 — uuid 인덱스 사용
    Optional<Member> findByUuid(UUID uuid);

    // 회원이 없으면 MEMBER_NOT_FOUND — 서비스마다 반복되던 orElseThrow를 한 곳으로 모음
    default Member findByUuidOrThrow(UUID uuid) {
        return findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    // 닉네임 중복 확인
    boolean existsByNickname(String nickname);
}
