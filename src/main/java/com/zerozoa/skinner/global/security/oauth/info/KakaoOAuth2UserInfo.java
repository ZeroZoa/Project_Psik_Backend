package com.zerozoa.skinner.global.security.oauth.info;

import java.util.Map;

public class KakaoOAuth2UserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes; // oauth2User.getAttributes() 원본
    private final Map<String, Object> kakaoAccount;
    private final Map<String, Object> profile;

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        // 카카오 데이터 구조 파싱 (Null 체크는 실무에서 더 꼼꼼히 하지만, 기본 구조는 이렇습니다)
        this.kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        this.profile = (Map<String, Object>) kakaoAccount.get("profile");
    }

    @Override
    public String getProviderId() {
        // 카카오 ID는 Long 타입으로 오기 때문에 String 변환 필요
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
