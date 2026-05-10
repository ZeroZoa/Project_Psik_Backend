package com.zerozoa.psik.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductUpdateRequest(

        @NotBlank(message = "제품명은 필수입니다.")
        @Size(max = 200)
        String name,

        @NotNull(message = "제조사는 필수입니다.")
        @Size(max = 100)
        String brand,

        @NotNull(message = "가격은 필수입니다.")
        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        Long price,

        String description,

        @Size(max = 1000)
        String link,

        @Size(max = 1000)
        String imageUrl
) {}