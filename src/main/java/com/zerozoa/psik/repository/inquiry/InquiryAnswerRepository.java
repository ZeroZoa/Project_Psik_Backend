package com.zerozoa.psik.repository.inquiry;

import com.zerozoa.psik.domain.inquiry.InquiryAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 1:1 문의 답변 Repository
 */
public interface InquiryAnswerRepository extends JpaRepository<InquiryAnswer, Long> {
}