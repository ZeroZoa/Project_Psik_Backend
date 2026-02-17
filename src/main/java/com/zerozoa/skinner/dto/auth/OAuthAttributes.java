package com.zerozoa.skinner.dto.auth;

import com.zerozoa.skinner.domain.member.Member;
import com.zerozoa.skinner.domain.member.Provider;
import com.zerozoa.skinner.domain.member.Role;
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
        //이메일, 전화번호, 프로필 전부 kakaoAccount에 있음
        //kakao_account 에서 이메일, 전화번호 등을 추출
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        //프로필 사진 추출
        Map<String, Object> kakaoProfile = (Map<String, Object>) kakaoAccount.get("profile");

        //OAuth ID 추출 (카카오 PK)
        String oauthId = String.valueOf(attributes.get(userNameAttributeName));
        //이메일 추출 시도 (나중에 심사 통과 후 YAML 주석만 풀면 자동으로 들어옴)
        String email = (String) kakaoAccount.get("email");
        //[Fallback] 이메일이 없는 경우(심사 전) -> '고유번호@kakao.social'로 임시 생성
        if (email == null || email.isBlank()) {
            email = oauthId + "@kakao.social";
        }

        String rawPhone = (String) kakaoAccount.get("phone_number");
        String formattedPhone = null;
        if (rawPhone != null) {
            formattedPhone = rawPhone.startsWith("+82 ")
                    ? rawPhone.replace("+82 ", "0")
                    : rawPhone;
        }

        return OAuthAttributes.builder()
                .provider(Provider.KAKAO)
                .oauthId(oauthId) // "id"
                //카카오 설정에 따라 profile이 없을수도 있기때문에 null체크
                .nickname(kakaoProfile != null ? (String) kakaoProfile.get("nickname") : null)
                .email(email)
                //.email((String) kakaoAccount.get("email"))
                .profileImageUrl(kakaoProfile != null ? (String) kakaoProfile.get("profile_image_url") : null)
                .phoneNumber(formattedPhone) // 국내 번호 포맷 (+82 10...)
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
                .nickname(nickname)
                .email(email)
                .profileImageUrl(profileImageUrl)
                .phoneNumber(phoneNumber)
                .role(Role.USER) // 가입 시 기본 권한 USER
                .build();
    }
}