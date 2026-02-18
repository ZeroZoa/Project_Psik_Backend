package com.zerozoa.skinner.dto.contents;

import com.zerozoa.skinner.domain.contents.Ingredient;
import com.zerozoa.skinner.domain.contents.Tag;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

@Builder
public record IngredientDetailResponse(
        Long id,
        String name,
        String typeTitle,       // "일반/화장품"
        String typeDescription, // "누구나 쉽게 구매 가능"
        String description,     // 전체 설명
        List<String> effects,   // 효과 리스트
        List<String> cautions,  // 주의사항 리스트
        List<String> tags,      // 태그 리스트
        List<ProductDto> products // 추천 제품 목록 (위에서 만든 DTO 재사용)
) {
    public static IngredientDetailResponse from(Ingredient ingredient) {
        return IngredientDetailResponse.builder()
                .id(ingredient.getId())
                .name(ingredient.getName())
                .typeTitle(ingredient.getType().getTitle())
                .typeDescription(ingredient.getType().getDescription())
                .description(ingredient.getDescription())
                .effects(new ArrayList<>(ingredient.getEffects()))
                .cautions(new ArrayList<>(ingredient.getCautions()))
                .tags(ingredient.getTags().stream()
                        .map(Tag::getName)
                        .toList())
                .products(ingredient.getProducts().stream()
                        .map(ProductDto::from)
                        .toList())
                .build();
    }
}