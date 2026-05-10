package com.zerozoa.psik.domain.member;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 성별 Enum
 * 회원 프로필 설정 시 선택
 */
@Getter
@RequiredArgsConstructor
public enum Gender {
    MALE("남성"),
    FEMALE("여성"),
    OTHER("기타");

    private final String description;
}
