package com.zerozoa.skinner.dto.contents;

import com.zerozoa.skinner.domain.contents.Ingredient;
import com.zerozoa.skinner.domain.contents.Tag;
import lombok.Builder;

import java.util.List;

@Builder
public record IngredientResponse(
        Long id,
        String name,
        String typeTitle,         // "일반/화장품" (화면 표시용)
        String descriptionSummary,// 목록에서는 설명이 너무 길면 잘라서 보여줌
        List<String> tags         // 태그 이름만 리스트로 (#여드름, #진정)
) {
    public static IngredientResponse from(Ingredient ingredient) {
        // 설명이 50자를 넘으면 "..." 붙여서 요약
        String summary = ingredient.getDescription();
        if (summary != null && summary.length() > 50) {
            summary = summary.substring(0, 50) + "...";
        }

        return IngredientResponse.builder()
                .id(ingredient.getId())
                .name(ingredient.getName())
                .typeTitle(ingredient.getType().getTitle()) // Enum의 title 필드 사용
                .descriptionSummary(summary)
                .tags(ingredient.getTags().stream()
                        .map(Tag::getName)
                        .toList())
                .build();
    }
}