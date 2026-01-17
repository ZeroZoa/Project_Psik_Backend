package com.zerozoa.skinner.global.security.oauth;

import com.zerozoa.skinner.domain.member.Member;
import com.zerozoa.skinner.domain.member.Provider;
import com.zerozoa.skinner.dto.auth.OAuthAttributes;
import com.zerozoa.skinner.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

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
        Map<String, Object> originAttributes = oauth2User.getAttributes();

        //OAuth2UserRequest객체에서 registrationId 추출 (kakao, naver, google)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        //OAuth2UserRequest객체에서 userNameAttributeName 추출
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        //Provider Enum 변환 (String -> Enum)
        Provider provider = getProvider(registrationId);

        //DTO 생성 (Factory Method Pattern)
        OAuthAttributes attributes = OAuthAttributes.of(provider, userNameAttributeName, originAttributes);

        //DB 저장 또는 업데이트 (Login & Signup)
        Member member = saveOrUpdate(attributes);

        //CustomUserDetails 반환 (Security Context에 저장됨)
        return new CustomOAuth2User(member, attributes.attributes());
    }

    //문자열(registrationId)을 Provider Enum으로 변환
    private Provider getProvider(String registrationId) {
        try {
            return Provider.valueOf(registrationId.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Unsupported Provider: {}", registrationId);
            // [수정] OAuth2AuthenticationException 생성 규격 준수 (OAuth2Error 객체 필요)
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_provider"),
                    "지원하지 않는 소셜 로그인입니다: " + registrationId
            );
        }
    }

    // 회원 가입 및 정보 업데이트 로직
    private Member saveOrUpdate(OAuthAttributes attributes) {
        // [성능] idx_member_oauth (provider + oauthId) 인덱스 활용
        return memberRepository.findByProviderAndOauthId(attributes.provider(), attributes.oauthId())
                .map(entity -> {
                    //정보 업데이트
                    entity.updateSocialInfo(attributes.email(), attributes.phoneNumber());
                    entity.updateProfile(attributes.nickname(), attributes.profileImageUrl());
                    return entity;
                })
                .orElseGet(() -> {
                    //DTO의 toEntity() 메서드 활용
                    return memberRepository.save(attributes.toEntity());
                });
    }
}
