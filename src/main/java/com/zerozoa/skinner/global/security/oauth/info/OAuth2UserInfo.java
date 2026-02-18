package com.zerozoa.skinner.global.security.oauth.info;

/**
 *OAuth2 사용자 정보 인터페이스
 *소셜 로그인 제공자(Google, Kakao, Naver)마다 다른 응답 JSON 구조를 통일된 규격으로 맞추기위한 interface
 *다형성을 활용하여 서비스 로직(CustomOAuth2UserService)에서는 구체적인 구현체(KakaoUserInfo 등)를 몰라도 됩니다.
 */
public interface OAuth2UserInfo {
    String getProviderId();       // 소셜 식별자 (예: 123456)
    String getProvider();         // google, kakao, naver
    String getEmail();            // 이메일
    String getName();             // 이름/닉네임
    String getProfileImageUrl();  // 프로필 사진 URL
}
