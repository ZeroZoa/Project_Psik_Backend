package com.zerozoa.skinner.global.security.oauth.info;

public interface OAuth2UserInfo {
    String getProviderId();       // 소셜 식별자 (예: 123456)
    String getProvider();         // google, kakao, naver
    String getEmail();            // 이메일
    String getName();             // 이름/닉네임
    String getProfileImageUrl();  // 프로필 사진 URL
}
