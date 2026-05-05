package com.zerozoa.psik.service;

import com.zerozoa.psik.domain.inquiry.Inquiry;
import com.zerozoa.psik.domain.inquiry.InquiryAnswer;
import com.zerozoa.psik.domain.member.Member;
import com.zerozoa.psik.dto.inquiry.InquiryAnswerRequest;
import com.zerozoa.psik.dto.inquiry.InquiryRequest;
import com.zerozoa.psik.dto.inquiry.InquiryResponse;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import com.zerozoa.psik.repository.inquiry.InquiryAnswerRepository;
import com.zerozoa.psik.repository.inquiry.InquiryRepository;
import com.zerozoa.psik.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryAnswerRepository inquiryAnswerRepository;
    private final MemberRepository memberRepository;

    /** 문의 등록 */
    @Transactional
    public InquiryResponse createInquiry(UUID memberUuid, InquiryRequest request) {
        Member member = memberRepository.findByUuid(memberUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));


        Inquiry inquiry = Inquiry.builder()
                .member(member)
                .title(request.title())
                .content(request.content())
                .build();

        return InquiryResponse.from(inquiryRepository.save(inquiry));
    }

    /** 내 문의 목록 조회 */
    public Page<InquiryResponse> getMyInquiries(UUID memberUuid, Pageable pageable) {
        return inquiryRepository.findByMemberUuidWithAnswer(memberUuid, pageable)
                .map(InquiryResponse::from);
    }

    /** 관리자 전체 문의 목록 조회 */
    public Page<InquiryResponse> getAllInquiries(Pageable pageable) {
        return inquiryRepository.findAllWithAnswer(pageable)
                .map(InquiryResponse::from);
    }

    /** 관리자 답변 등록 */
    @Transactional
    public InquiryResponse createAnswer(Long inquiryId, InquiryAnswerRequest request) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        if (inquiry.isAnswered()) {
            throw new BusinessException(ErrorCode.INQUIRY_ALREADY_ANSWERED);
        }

        InquiryAnswer answer = InquiryAnswer.builder()
                .inquiry(inquiry)
                .content(request.content())
                .build();

        inquiryAnswerRepository.save(answer);

        // 답변 포함해서 다시 조회
        return InquiryResponse.from(
                inquiryRepository.findById(inquiryId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND))
        );
    }
}