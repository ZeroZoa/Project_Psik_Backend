package com.zerozoa.psik.dto.auth;

import com.zerozoa.psik.domain.member.Member;
import com.zerozoa.psik.domain.member.Provider;
import com.zerozoa.psik.domain.member.Role;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
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

    // Provider에 따라 알맞은 팩토리 메서드 호출
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
                .oauthId(String.valueOf(attributes.get(userNameAttributeName))) // "sub"
                .nickname((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .profileImageUrl((String) attributes.get("picture"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {

        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount == null) {
            // 계정 정보가 없으면 빈 맵으로 초기화하여 null 값 방지
            kakaoAccount = Map.of();
        }

        //프로필 null 확인
        Map<String, Object> kakaoProfile = (Map<String, Object>) kakaoAccount.get("profile");

        //OAuth ID 추출
        String oauthId = String.valueOf(attributes.get(userNameAttributeName));

        //이메일 Fallback 로직 (유지)
        String email = (String) kakaoAccount.get("email");
        if (email == null || email.isBlank()) {
            email = oauthId + "@kakao.social"; // 임시 이메일 발급
        }

        //전화번호 substring
        String rawPhone = (String) kakaoAccount.get("phone_number");
        String formattedPhone = null;
        if (rawPhone != null) {
            formattedPhone = rawPhone.startsWith("+82 ")
                    ? "0" + rawPhone.substring(4)
                    : rawPhone;
        }

        return OAuthAttributes.builder()
                .provider(Provider.KAKAO)
                .oauthId(oauthId)
                .nickname(kakaoProfile != null ? (String) kakaoProfile.get("nickname") : "Unknown") // 닉네임 없을 경우 기본값
                .email(email)
                .profileImageUrl(kakaoProfile != null ? (String) kakaoProfile.get("profile_image_url") : null)
                .phoneNumber(formattedPhone)
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        // 네이버는 response라는 키 안에 모든 정보가 담겨옴
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        return OAuthAttributes.builder()
                .provider(Provider.NAVER)
                .oauthId(String.valueOf(response.get("id")))
                .nickname((String) response.get("nickname"))
                .email((String) response.get("email"))
                .profileImageUrl((String) response.get("profile_image"))
                .phoneNumber((String) response.get("mobile")) // 포맷: 010-0000-0000
                .attributes(attributes)
                .nameAttributeKey("id") // 네이버는 식별자가 내부의 id 값임
                .build();
    }

    private static OAuthAttributes ofApple(String userNameAttributeName, Map<String, Object> attributes) {
        // [주의] 애플은 최초 로그인 시에만 email, name을 제공
        // 2번째 로그인부터는 'sub'(oauthId)만 주므로, null 체크가 필수입니다.
        // 실무에서는 별도의 AppleClient를 구현하여 identity_token을 디코딩하는 방식을 권장합니다.
        return OAuthAttributes.builder()
                .provider(Provider.APPLE)
                .oauthId(String.valueOf(attributes.get("sub")))
                .email((String) attributes.get("email")) // 2회차 로그인부터 null일 수 있음
                .attributes(attributes)
                .nameAttributeKey("sub")
                .build();
    }

    // DTO -> Entity 변환
    public Member toEntity() {
        return Member.builder()
                .provider(provider)
                .oauthId(oauthId)
                .nickname(generateTempNickname())
                .email(email)
                .profileImageUrl(profileImageUrl)
                .phoneNumber(phoneNumber)
                .role(Role.USER) // 가입 시 기본 권한 USER
                .build();
    }

    //닉네임 중복을 막기위해
    private String generateTempNickname() {
        String uid = java.util.UUID.randomUUID().toString().replace("-", "");
        return "user_" + uid.substring(0, 6);
    }
}