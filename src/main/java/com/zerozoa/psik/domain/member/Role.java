package com.zerozoa.psik.domain.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

//회원의 권한을 정의하는 Enum 클래스
@Getter
@RequiredArgsConstructor
public enum Role {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");

    private final String key;
}
