package com.zerozoa.skinner.dto.admin;

import com.zerozoa.skinner.domain.contents.IngredientType;
import com.zerozoa.skinner.domain.member.SkinConcern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record IngredientUpdateRequest(

        @NotBlank(message = "성분명은 필수입니다.")
        @Size(max = 100)
        String name,

        @NotNull(message = "성분 타입은 필수입니다.")
        IngredientType type,

        @Size(max = 100)
        String effectSummary,

        @NotBlank(message = "성분 설명은 필수입니다.")
        String description,

        @NotNull
        List<String> effects,

        @NotNull
        List<String> cautions,

        @NotNull
        List<SkinConcern> skinConcerns
) {}