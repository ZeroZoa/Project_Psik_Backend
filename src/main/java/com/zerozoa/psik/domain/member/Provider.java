package com.zerozoa.psik.domain.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 소셜 로그인 제공자 Enum
 * 현재 GOOGLE, KAKAO만 구현되어 있으며 NAVER, APPLE은 추후 앱스토어 출시 시 지원 예정
 */
@Getter
@RequiredArgsConstructor
public enum Provider {

    /** 미구현 - 추후 앱스토어 출시 시 지원 예정 */
    NAVER("네이버"),

    /** 미구현 - 추후 앱스토어 출시 시 지원 예정 */
    APPLE("애플"),

    GOOGLE("구글"),
    KAKAO("카카오");

    private final String description;
}
