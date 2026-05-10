package com.zerozoa.psik.domain.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

//회원의 권한을 정의하는 Enum 클래스
@Getter
@RequiredArgsConstructor
public enum Role {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");

    // Spring Security 인가에서 사용하는 접두사 포함 권한명 (예: "ROLE_USER")
    private final String key;
}
