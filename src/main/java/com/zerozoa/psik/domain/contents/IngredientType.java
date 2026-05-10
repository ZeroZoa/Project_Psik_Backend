package com.zerozoa.psik.domain.contents;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 성분(Ingredient) 유형 Enum
 * 성분 필터링 기준 및 프론트엔드 탭 메뉴로 활용
 */
@Getter
@RequiredArgsConstructor
public enum IngredientType {

    /** 일반 화장품 성분 — 누구나 쉽게 구매 가능 */
    GENERAL("일반/화장품", "누구나 쉽게 구매 가능"),

    /** 일반의약품 성분 — 약국에서만 구매 가능 */
    OTC("일반의약품/약국", "약국에서 구매 가능"),

    /** 전문의약품 성분 — 의사 처방 필수 */
    PRESCRIPTION("전문의약품/병원", "의사 처방 필요"),

    /** 해외 직구 성분 — 국내에서는 처방이 필요하지만 해외에서는 자유 구매 가능 */
    OVERSEAS("해외직구/직수입", "해외 직구로 구매 가능");

    private final String title;       // 화면 표시용 레이블
    private final String description; // 상세 설명 (툴팁 등에 활용)
}
