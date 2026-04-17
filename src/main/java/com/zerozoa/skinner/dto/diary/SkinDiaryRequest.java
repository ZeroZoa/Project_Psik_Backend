package com.zerozoa.skinner.dto.diary;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

//스킨 다이어리 생성 및 수정 요청 DTO
public record SkinDiaryRequest(

        @NotNull(message = "기록 날짜는 필수입니다.")
        Instant recordDate,

        @NotNull(message = "피부 점수는 필수입니다.")
        @Min(value = 1, message = "점수는 1 이상이어야 합니다.")
        @Max(value = 5, message = "점수는 5 이하이어야 합니다.")
        Integer skinScore,

        // 수면 시간 (분 단위)
        Integer sleepTimeMinutes,

        // 물 섭취량 (ml 단위)
        Integer waterIntakeMl,

        // 식단 리스트 (예: ["비빔밥", "치킨"])
        List<String> diet,

        // 프론트엔드에서는 선택한 화장품(Product)의 PK(ID) 배열만 넘겨줍니다.
        // 서비스 로직에서 이 ID들로 Product 엔티티를 조회하여 매핑합니다.
        List<Long> usedProductIds
) {
}