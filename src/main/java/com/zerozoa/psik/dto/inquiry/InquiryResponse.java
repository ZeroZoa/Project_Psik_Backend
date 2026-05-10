package com.zerozoa.psik.dto.inquiry;

import com.zerozoa.psik.domain.inquiry.Inquiry;
import com.zerozoa.psik.domain.inquiry.InquiryAnswer;

import java.time.Instant;

public record InquiryResponse(
        Long id,
        String title,
        String content,
        boolean answered,           // answer != null 이면 true
        String answerContent,       // 답변 내용 (없으면 null)
        Instant answeredAt,         // 답변 작성일 (없으면 null)
        String authorNickname,
        Instant createdAt
) {
    public static InquiryResponse from(Inquiry inquiry) {
        var answer = inquiry.getAnswer();
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.isAnswered(),
                answer != null ? answer.getContent() : null,
                answer != null ? answer.getCreatedAt() : null,
                inquiry.getMember().getNickname(),
                inquiry.getCreatedAt()
        );
    }

    public static InquiryResponse fromWithAnswer(Inquiry inquiry, InquiryAnswer answer) {
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getTitle(),
                inquiry.getContent(),
                true,
                answer.getContent(),
                answer.getCreatedAt(),
                inquiry.getMember().getNickname(),
                inquiry.getCreatedAt()
        );
    }
}