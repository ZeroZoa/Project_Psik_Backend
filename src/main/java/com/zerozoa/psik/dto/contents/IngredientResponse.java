package com.zerozoa.psik.dto.contents;

import com.zerozoa.psik.domain.contents.Ingredient;
import com.zerozoa.psik.domain.member.SkinConcern;
import lombok.Builder;

import java.util.List;

@Builder
public record IngredientResponse(
        Long id,
        String name,
        String typeTitle,
        String effectSummary,
        String descriptionSummary,
        List<String> skinConcerns
) {
    public static IngredientResponse from(Ingredient ingredient) {
        String summary = ingredient.getDescription();
        if (summary != null && summary.length() > 50) {
            summary = summary.substring(0, 50) + "...";
        }

        return IngredientResponse.builder()
                .id(ingredient.getId())
                .name(ingredient.getName())
                .typeTitle(ingredient.getType().getTitle())
                .effectSummary(ingredient.getEffectSummary())
                .descriptionSummary(summary)
                .skinConcerns(ingredient.getSkinConcerns().stream()
                        .map(SkinConcern::getDescription)
                        .toList())
                .build();
    }
}