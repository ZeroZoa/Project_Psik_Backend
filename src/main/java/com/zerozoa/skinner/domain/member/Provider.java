package com.zerozoa.skinner.domain.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Provider {
    GOOGLE("구글"),
    KAKAO("카카오"),
    NAVER("네이버"),
    APPLE("애플");

    private final String description;
}
