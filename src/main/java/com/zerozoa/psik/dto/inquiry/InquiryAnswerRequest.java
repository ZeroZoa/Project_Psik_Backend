package com.zerozoa.psik.dto.inquiry;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryAnswerRequest(
        @NotBlank(message = "답변 내용을 입력해주세요.")
        @Size(max = 2000, message = "답변은 2000자 이내로 작성해주세요.")
        String content
) {}