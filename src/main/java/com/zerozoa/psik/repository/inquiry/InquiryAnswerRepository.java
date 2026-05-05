package com.zerozoa.psik.repository.inquiry;

import com.zerozoa.psik.domain.inquiry.InquiryAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryAnswerRepository extends JpaRepository<InquiryAnswer, Long> {
    boolean existsByInquiryId(Long inquiryId);
}