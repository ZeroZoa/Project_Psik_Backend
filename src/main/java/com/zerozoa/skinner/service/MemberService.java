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
        //DB에서 Provider, oauthId -> 복합인덱스를 통해 회원 조회
        return memberRepository.findByProviderAndOauthId(attributes.provider(), attributes.oauthId())
                //이미 있다면 로그인 성공 처리
                .map(member -> {
                    // Provider에서 이메일이나 전화번호가 업데이트되었을수도 있기떄문에정보를 동기화합니다.
                    member.updateSocialInfo(attributes.email(), attributes.phoneNumber());
                    log.info("[Login] Member Login Success: uuid={}", member.getUuid());
                    return member;
                })
                //없다면 회원가입 처리
                .orElseGet(() -> {
                    // OAuthAttributes DTO를 Member 엔티티로 변환하여 DB에 저장합니다.
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
        refreshTokenRepository.deleteById(uuid);

        //회원 삭제 (Hard Delete)
        memberRepository.delete(member);

        log.info("[Withdraw] Member Deleted: uuid={}", uuid);
    }
}

