package com.zerozoa.skinner.service;

import com.zerozoa.skinner.domain.member.Member;
import com.zerozoa.skinner.dto.auth.OAuthAttributes;
import com.zerozoa.skinner.global.exception.MemberNotFoundException;
import com.zerozoa.skinner.repository.MemberRepository;
import com.zerozoa.skinner.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    //소셜 로그인 (회원가입/로그인 처리)
    @Transactional
    public Member socialLogin(OAuthAttributes attributes) {
        return memberRepository.findByProviderAndOauthId(attributes.provider(), attributes.oauthId())
                .map(member -> {
                    // [기존 회원] 정보 동기화 (Dirty Checking)
                    member.updateSocialInfo(attributes.email(), attributes.phoneNumber());
                    log.info("[Login] Member Login Success: uuid={}", member.getUuid());
                    return member;
                })
                .orElseGet(() -> {
                    // [신규 회원] 저장
                    Member savedMember = memberRepository.save(attributes.toEntity());
                    log.info("[Join] New Member Joined: uuid={}, provider={}", savedMember.getUuid(), savedMember.getProvider());
                    return savedMember;
                });
    }

    //UUID로 회원 단건 조회
    public Member getByUuid(UUID uuid) {
        return memberRepository.findByUuid(uuid)
                .orElseThrow(() -> {
                    log.warn("[Service] Member not found. uuid={}", uuid);
                    // [Refactor] 명확한 커스텀 예외 사용 (404 Status로 매핑하기 위함)
                    return new MemberNotFoundException(uuid);
                });
    }

    //닉네임 중복 확인
    public boolean isNicknameDuplicate(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }

    //회원 탈퇴
    @Transactional
    public void deleteMember(UUID uuid) {
        Member member = getByUuid(uuid); // 존재 확인 (없으면 예외 발생)

        //Refresh Token 삭제 필수
        refreshTokenRepository.deleteById(uuid.toString());

        //회원 삭제 (Hard Delete)
        memberRepository.delete(member);

        log.info("[Withdraw] Member Deleted: uuid={}", uuid);
    }
}

