package com.zerozoa.skinner.repository.member;

import com.zerozoa.skinner.domain.member.Member;
import com.zerozoa.skinner.domain.member.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface MemberRepository extends JpaRepository<Member, Long> {

    //소셜 로그인 시 회원 조회
    //Member엔티티의 복합 인덱스(제공자 + id)를 사용
    Optional<Member> findByProviderAndOauthId(Provider provider, String oauthId);

    //토큰 검증 후 회원 조회
    //UUID를 통해 회원 조회
    Optional<Member> findByUuid(UUID uuid);

    //닉네임 중복 확인
    boolean existsByNickname(String nickname);
}
