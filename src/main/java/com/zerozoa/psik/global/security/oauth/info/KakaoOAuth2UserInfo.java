package com.zerozoa.psik.global.security.oauth.info;

import java.util.Map;

/**
 *카카오 사용자 정보 구현체
 *카카오의 사용자 정보 Response는 계층형 JSON 구조를 가집니다.
 */
public class KakaoOAuth2UserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes; // oauth2User.getAttributes() 원본
    private final Map<String, Object> kakaoAccount;
    private final Map<String, Object> profile;

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        //kakao_account 추출
        this.kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

        this.profile = (Map<String, Object>) kakaoAccount.get("profile");
    }

    @Override
    public String getProviderId() {
        // 카카오 ID는 Long 타입으로 오기 때문에 String 변환
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getEmail() {
        if (kakaoAccount == null) return null;
        return (String) kakaoAccount.get("email");
    }

    @Override
    public String getName() {
        if (profile == null) return null;
        return (String) profile.get("nickname");
    }

    @Override
    public String getProfileImageUrl() {
        if (profile == null) return null;
        return (String) profile.get("profile_image_url");
    }
}
