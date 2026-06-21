package com.zerozoa.psik.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "질문을 입력해주세요.")
        @Size(max = 500, message = "질문은 500자 이내로 입력해주세요.")
        String message
) {}