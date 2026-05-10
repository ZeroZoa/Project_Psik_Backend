package com.zerozoa.psik.controller;

import com.zerozoa.psik.dto.inquiry.InquiryAnswerRequest;
import com.zerozoa.psik.dto.inquiry.InquiryRequest;
import com.zerozoa.psik.dto.inquiry.InquiryResponse;
import com.zerozoa.psik.global.util.SecurityUtils;
import com.zerozoa.psik.service.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@Tag(name = "Inquiry API", description = "문의하기 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    /**
     * 문의 등록
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입)
     * @param request 문의 등록 요청 DTO
     * @return 201 Created - 등록된 문의 정보
     * @see InquiryService#createInquiry(UUID, InquiryRequest)
     */
    @Operation(summary = "문의 등록")
    @PostMapping
    public ResponseEntity<InquiryResponse> createInquiry(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @Valid @RequestBody InquiryRequest request
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        log.info("[API] 문의 등록 요청 - memberUuid={}", memberUuid);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inquiryService.createInquiry(memberUuid, request));
    }

    /**
     * 내 문의 목록 조회
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입)
     * @param pageable 페이지네이션 정보 (기본값: size=20)
     * @return 200 OK - 내 문의 목록 (페이지)
     * @see InquiryService#getMyInquiries(UUID, Pageable)
     */
    @Operation(summary = "내 문의 목록")
    @GetMapping("/mine")
    public ResponseEntity<Page<InquiryResponse>> getMyInquiries(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        return ResponseEntity.ok(inquiryService.getMyInquiries(memberUuid, pageable));
    }

    /**
     * 전체 문의 목록 조회 (관리자 전용)
     * @param pageable 페이지네이션 정보 (기본값: size=20)
     * @return 200 OK - 전체 문의 목록 (페이지)
     * @see InquiryService#getAllInquiries(Pageable)
     */
    @Operation(summary = "전체 문의 목록 (관리자)")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<InquiryResponse>> getAllInquiries(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(inquiryService.getAllInquiries(pageable));
    }

    /**
     * 문의 답변 등록 (관리자 전용)
     * @param inquiryId 답변을 등록할 문의의 ID
     * @param request 답변 등록 요청 DTO
     * @return 201 Created - 답변이 포함된 문의 정보
     * @see InquiryService#createAnswer(Long, InquiryAnswerRequest)
     */
    @Operation(summary = "문의 답변 등록 (관리자)")
    @PostMapping("/{inquiryId}/answer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InquiryResponse> createAnswer(
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryAnswerRequest request
    ) {
        log.info("[API] 문의 답변 등록 요청 - inquiryId={}", inquiryId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inquiryService.createAnswer(inquiryId, request));
    }
}