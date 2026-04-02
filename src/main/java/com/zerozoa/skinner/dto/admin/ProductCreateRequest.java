package com.zerozoa.skinner.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(

        @NotBlank(message = "제품명은 필수입니다.")
        @Size(max = 200)
        String name,

        @Size(max = 100)
        String brand,

        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        Long price,

        String description,

        @Size(max = 1000)
        String link,

        @Size(max = 1000)
        String imageUrl
) {}