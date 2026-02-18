package com.zerozoa.skinner.domain.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

//소셜 로그인 제공자를 정의하는 Enum 클래스
@Getter
@RequiredArgsConstructor
public enum Provider {
    GOOGLE("구글"),
    KAKAO("카카오"),
    NAVER("네이버"),
    APPLE("애플");

    private final String description;
}
