package com.zerozoa.psik.domain.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 피부 타입 Enum
 * 회원 프로필 설정 시 선택하며, 성분 추천 필터링에 활용됨
 */
@Getter
@RequiredArgsConstructor
public enum SkinType {
    DRY("건성"),
    OILY("지성"),
    COMBINATION("복합성"),
    SENSITIVE("민감성"),
    NORMAL("중성");

    private final String description;
}