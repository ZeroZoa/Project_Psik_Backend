package com.zerozoa.skinner.global.security.oauth;

import com.zerozoa.skinner.domain.member.Member;
import com.zerozoa.skinner.domain.member.Provider;
import com.zerozoa.skinner.domain.member.Role;
import com.zerozoa.skinner.global.security.oauth.info.KakaoOAuth2UserInfo;
import com.zerozoa.skinner.global.security.oauth.info.OAuth2UserInfo;
import com.zerozoa.skinner.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        //소셜 서비스에서 유저 정보 가져오기
        OAuth2User oauth2User = super.loadUser(userRequest);

        //registrationId 추출
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        //Provider Enum 변환 (String -> Enum 안전 변환)
        Provider provider = getProvider(registrationId);

        // 유저 정보 객체 생성 (Factory 패턴 적용 가능하나 지금은 if문 처리)
        OAuth2UserInfo userInfo;
        if (provider == Provider.KAKAO) {
            userInfo = new KakaoOAuth2UserInfo(oauth2User.getAttributes());
        } else {
            // 추후 구글, 애플 등 확장 시 이곳에 추가
            log.error("지원하지 않는 소셜 로그인입니다. RegistrationId: {}", registrationId);
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다: " + registrationId);
        }

        //DB 저장 또는 업데이트 (Login & Signup)
        Member member = saveOrUpdate(userInfo, provider);

        //CustomUserDetails 반환 (Security Context에 저장됨)
        return new CustomOAuth2User(member, oauth2User.getAttributes());
    }

    //문자열(registrationId)을 Provider Enum으로 변환
    private Provider getProvider(String registrationId) {
        try {
            // "kakao" -> "KAKAO"로 변환하여 Enum 찾기
            return Provider.valueOf(registrationId.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Unknown Provider: {}", registrationId);
            throw new OAuth2AuthenticationException("지원하지 않는 Provider입니다.");
        }
    }

    //회원 가입 및 정보 업데이트 로직
    private Member saveOrUpdate(OAuth2UserInfo userInfo, Provider provider) {
        // [핵심] idx_member_oauth (provider + oauthId) 복합 인덱스를 사용하여 조회 성능 최적화
        return memberRepository.findByProviderAndOauthId(provider, userInfo.getProviderId())
                .map(entity -> {
                    // 1. 기존 회원: 변경된 프로필 정보(닉네임, 프사)가 있다면 업데이트
                    entity.updateProfile(userInfo.getName(), userInfo.getProfileImageUrl());

                    // 2. 이메일 등 소셜 정보 동기화 (전화번호는 카카오 설정에 따라 다름, 여기선 null 처리 or 로직 추가)
                    entity.updateSocialInfo(userInfo.getEmail(), null);

                    return entity;
                })
                .orElseGet(() -> {
                    // 3. 신규 회원: Member 객체 생성 (빌더 패턴)
                    Member newMember = Member.builder()
                            .provider(provider)                // Enum
                            .oauthId(userInfo.getProviderId()) // 소셜 식별자
                            .nickname(userInfo.getName())
                            .profileImageUrl(userInfo.getProfileImageUrl())
                            .email(userInfo.getEmail())
                            .role(Role.USER)                   // 가입 시 기본 권한은 USER
                            .build();

                    // UUID 생성 로직은 Member 엔티티의 @PrePersist가 담당
                    return memberRepository.save(newMember);
                });
    }
}
