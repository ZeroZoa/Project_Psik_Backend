package com.zerozoa.skinner.domain.contents;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

//성분(Ingredient)의 유형을 정의하는 Enum 클래스입니다.
//Filter의 기준, FrontEnd의 탭 메뉴로 활용
@Getter
@RequiredArgsConstructor
public enum IngredientType {
    //누구나 쉽게 구매
    GENERAL("일반/화장품", "누구나 쉽게 구매 가능"),

    //약국에서만 구매
    OTC("일반의약품/약국", "약국에서 구매 가능"),

    //의사 처방 필수
    PRESCRIPTION("전문의약품/병원", "의사 처방 필요"),

    //국내에선 처방 필요하지만, 해외에선 그냥 구매 가능 (직구)
    OVERSEAS("해외직구/직수입", "해외 직구로 구매 가능");

    private final String title;// 화면 표시용
    private final String description; // 상세 설명
}
