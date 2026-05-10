package com.zerozoa.psik.repository.inquiry;

import com.zerozoa.psik.domain.inquiry.InquiryAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 1:1 문의 답변 Repository
 */
public interface InquiryAnswerRepository extends JpaRepository<InquiryAnswer, Long> {
    // 문의 ID로 답변 존재 여부 확인 (중복 답변 방지)
    boolean existsByInquiryId(Long inquiryId);
}