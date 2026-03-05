package com.zerozoa.skinner.dto.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 100, message = "제목은 100자 이내로 작성해주세요.")
        String title,

        @NotBlank(message = "내용을 입력해주세요.")
        String content
) {}