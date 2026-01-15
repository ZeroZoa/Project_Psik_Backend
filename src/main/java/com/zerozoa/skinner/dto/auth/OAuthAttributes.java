package com.zerozoa.skinner.dto.auth;

import com.zerozoa.skinner.domain.member.Member;
import com.zerozoa.skinner.domain.member.Provider;
import com.zerozoa.skinner.domain.member.Role;
import lombok.Builder;

import java.util.Map;

@Builder
public record OAuthAttributes(
        Map<String, Object> attributes, // 소셜에서 준 원본 JSON 데이터
        String nameAttributeKey,        // 소셜에서 사용하는 PK 키값 (예: id, sub)
        Provider provider,
        String oauthId,
        String nickname,
        String email,
        String profileImageUrl,
        String phoneNumber
) {

    //provider에 따라 알맞은 메서드를 호출하여 객체를 생성함
    public static OAuthAttributes of(Provider provider, String userNameAttributeName, Map<String, Object> attributes) {
        return switch (provider) {
            case KAKAO -> ofKakao(userNameAttributeName, attributes);
            case NAVER -> ofNaver(userNameAttributeName, attributes);
            case GOOGLE -> ofGoogle(userNameAttributeName, attributes);
            case APPLE -> ofApple(userNameAttributeName, attributes);
        };
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .provider(Provider.GOOGLE)
                .oauthId(String.valueOf(attributes.get(userNameAttributeName))) //sub
                .nickname((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .profileImageUrl((String) attributes.get("picture"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        // 카카오는 kakao_account 안에 유저 정보가 있고, properties 안에 닉네임이 있음 (구조 복잡)
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> kakaoProfile = (Map<String, Object>) kakaoAccount.get("profile");

        return OAuthAttributes.builder()
                .provider(Provider.KAKAO)
                .oauthId(String.valueOf(attributes.get(userNameAttributeName))) // "id"
                .nickname((String) kakaoProfile.get("nickname"))
                .email((String) kakaoAccount.get("email")) // 동의 안하면 null일 수 있음
                .profileImageUrl((String) kakaoProfile.get("profile_image_url"))
                .phoneNumber((String) kakaoAccount.get("phone_number")) // 카카오 비즈 앱 아니면 못 받을 수 있음
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        // 네이버 response=라는 키 안에 모든 정보가 담겨옴
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        return OAuthAttributes.builder()
                .provider(Provider.NAVER)
                .oauthId((String) response.get("id"))
                .nickname((String) response.get("nickname"))
                .email((String) response.get("email"))
                .profileImageUrl((String) response.get("profile_image"))
                .phoneNumber((String) response.get("mobile"))
                .attributes(attributes)
                .nameAttributeKey("id")
                .build();
    }

    // 애플은 JWT(identity_token)를 디코딩해서 정보를 얻으므로 로직이 조금 다릅니다. (필요 시 추후 구현)
    private static OAuthAttributes ofApple(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .provider(Provider.APPLE)
                .oauthId(String.valueOf(attributes.get("sub")))
                .email((String) attributes.get("email"))
                .attributes(attributes)
                .nameAttributeKey("sub")
                .build();
    }

    //담겨온 정보를 Entity로 저장
    public Member toEntity() {
        return Member.builder()
                .provider(provider)
                .oauthId(oauthId)
                .nickname(nickname)
                .email(email)
                .profileImageUrl(profileImageUrl)
                .phoneNumber(phoneNumber)
                .role(Role.USER) // 가입 시 기본 권한 USER
                .build();
    }
}
