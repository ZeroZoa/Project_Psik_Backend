package com.zerozoa.psik.dto.contents;

import com.zerozoa.psik.domain.contents.Ingredient;
import com.zerozoa.psik.domain.member.SkinConcern;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

@Builder
public record IngredientDetailResponse(
        Long id,
        String name,
        String typeTitle,
        String typeDescription,
        String effectSummary,
        String description,
        List<String> effects,
        List<String> cautions,
        List<String> skinConcerns,
        List<ProductDto> products
) {
    public static IngredientDetailResponse from(Ingredient ingredient) {
        return IngredientDetailResponse.builder()
                .id(ingredient.getId())
                .name(ingredient.getName())
                .typeTitle(ingredient.getType().getTitle())
                .typeDescription(ingredient.getType().getDescription())
                .effectSummary(ingredient.getEffectSummary())
                .description(ingredient.getDescription())
                .effects(new ArrayList<>(ingredient.getEffects()))
                .cautions(new ArrayList<>(ingredient.getCautions()))
                .skinConcerns(ingredient.getSkinConcerns().stream()
                        .map(SkinConcern::getDescription)
                        .toList())
                .products(ingredient.getProducts().stream()
                        .map(ProductDto::from)
                        .toList())
                .build();
    }
}