package com.zerozoa.skinner.repository;

import com.zerozoa.skinner.domain.member.Member;
import com.zerozoa.skinner.domain.member.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByProviderAndOauthId(Provider provider, String oauthId);

    Optional<Member> findByUuid(UUID uuid);

    boolean existsByNickname(String nickname);
}
