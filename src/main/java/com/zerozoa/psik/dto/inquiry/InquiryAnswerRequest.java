package com.zerozoa.psik.dto.inquiry;

import jakarta.validation.constraints.NotBlank;

public record InquiryAnswerRequest(
        @NotBlank(message = "답변 내용을 입력해주세요.")
        String content
) {}