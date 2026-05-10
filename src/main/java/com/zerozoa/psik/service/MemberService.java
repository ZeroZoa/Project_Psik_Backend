package com.zerozoa.psik.service;

import com.zerozoa.psik.domain.member.Member;
import com.zerozoa.psik.dto.auth.OAuthAttributes;
import com.zerozoa.psik.dto.member.ProfileSetupRequest;
import com.zerozoa.psik.dto.member.SkinConcernUpdateRequest;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import com.zerozoa.psik.repository.member.MemberRepository;
import com.zerozoa.psik.repository.auth.RefreshTokenRepository;
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

    /**
     * 소셜 로그인 처리 (로그인 & 회원가입 겸용)
     * @param attributes 소셜 서비스(카카오 등)에서 받은 사용자 프로필 정보
     * @return 로그인 또는 회원가입이 완료된 {@link Member} 엔티티
     */
    @Transactional
    public Member socialLogin(OAuthAttributes attributes) {
        //DB에서 Provider, oauthId -> 복합인덱스를 통해 회원 조회
        return memberRepository.findByProviderAndOauthId(attributes.provider(), attributes.oauthId())
                //이미 회원가입되어 있다면 로그인 성공 처리
                .map(member -> {
                    // Provider에서 이메일이나 전화번호가 업데이트되었을수도 있기떄문에정보를 동기화
                    member.updateSocialInfo(attributes.email(), attributes.phoneNumber());
                    log.info("[Login] Member Login Success: uuid={}", member.getUuid());
                    return member;
                })
                //없다면 회원가입 처리
                .orElseGet(() -> {
                    // OAuthAttributes DTO를 Member 엔티티로 변환하여 DB에 저장
                    Member savedMember = memberRepository.save(attributes.toEntity());
                    log.info("[Join] New Member Joined: uuid={}, provider={}", savedMember.getUuid(), savedMember.getProvider());
                    return savedMember;
                });
    }

    /**
     * 회원 정보를 조회
     * @param memberUuid 조회할 회원의 UUID (API 토큰에서 추출한 값)
     * @return 조회된 {@link Member} 엔티티
     * @throws BusinessException 회원을 찾을 수 없는 경우 {@link ErrorCode#MEMBER_NOT_FOUND} 예외 발생
     */
    public Member getByUuid(UUID memberUuid) {
        return memberRepository.findByUuid(memberUuid)
                .orElseThrow(() -> {
                    log.warn("[Service] Member not found. uuid={}", memberUuid);
                    return new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
                });
    }

    /**
     * 닉네임 중복 확인
     * @param nickname 중복 검사할 닉네임
     * @return 중복이면 {@code true}, 사용 가능하면 {@code false}
     */
    public boolean isNicknameDuplicate(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }

    /**
     * 닉네임 수정
     * @param memberUuid 조회할 회원의 UUID (API 토큰에서 추출한 값)
     * @param nickname 변경할 닉네임
     * @return {@link Member} 엔티티
     * @throws BusinessException 닉네임이 중복인 경우 {@link ErrorCode#NICKNAME_ALREADY_EXISTS} 예외 발생
     */
    @Transactional
    public Member updateNickname(UUID memberUuid, String nickname) {
        Member member = getByUuid(memberUuid);

        // 현재 닉네임과 같으면 그냥 반환
        if (nickname.equals(member.getNickname())) {
            return member;
        }

        // 중복 체크
        if (memberRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        member.updateProfile(nickname, null);
        return member;
    }

    /**
     * 피부고민 수정
     * @param memberUuid 피부고민을 수정할 회원의 UUID
     * @param request 피부고민 설정 요청 DTO
     * @return {@link Member} 엔티티
     */
    @Transactional
    public Member updateSkinConcerns(UUID memberUuid, SkinConcernUpdateRequest request) {
        Member member = getByUuid(memberUuid);
        member.updateSkinConcerns(request.skinConcerns());
        return member; // 영속 상태 엔티티 → dirty checking이 트랜잭션 커밋 시 자동 UPDATE
    }

    /**
     * 최초 프로필 설정 (소셜 로그인 신규 회원 전용)
     * @param memberUuid 설정할 회원의 UUID
     * @param request 프로필 설정 요청 DTO
     * @return 설정 완료된 {@link Member}
     * @throws BusinessException profileComplete 가 이미 true 인 경우 {@link ErrorCode#PROFILE_ALREADY_COMPLETE}
     * @throws BusinessException 닉네임 중복인 경우 {@link ErrorCode#NICKNAME_ALREADY_EXISTS}
     */
    @Transactional
    public Member setupProfile(UUID memberUuid, ProfileSetupRequest request) {
        Member member = getByUuid(memberUuid);

        // 이미 프로필 설정 완료된 회원은 재설정 불가
        if (member.isProfileComplete()) {
            throw new BusinessException(ErrorCode.PROFILE_ALREADY_COMPLETE);
        }
        
        // 닉네임 중복 검사 (본인 닉네임 제외)
        if (!request.nickname().equals(member.getNickname())
                && memberRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        member.setupProfile(
                request.nickname(),
                request.gender(),
                request.birthYear(),
                request.skinType(),
                request.skinConcerns()
        );

        log.info("[Profile] Setup Complete: uuid={}, nickname={}", memberUuid, request.nickname());
        return member;
    }

    /**
     * 회원 탈퇴
     * @param memberUuid 탈퇴할 회원의 UUID
     * @throws BusinessException 회원이 존재하지 않을 경우 {@link ErrorCode#MEMBER_NOT_FOUND}
     */
    @Transactional
    public void deleteMember(UUID memberUuid) {
        //회원 존재 확인
        Member member = getByUuid(memberUuid);

        //연관된 토큰 일괄 삭제
        refreshTokenRepository.deleteAllByMemberUuid(memberUuid);

        //회원 데이터 삭제 (Hard Delete)
        memberRepository.delete(member);

        log.info("[Withdraw] Member Deleted: uuid={}", memberUuid);
    }
}