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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Inquiry API", description = "문의하기 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    /** 문의 등록 (로그인 필요) */
    @Operation(summary = "문의 등록")
    @PostMapping
    public ResponseEntity<InquiryResponse> createInquiry(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @Valid @RequestBody InquiryRequest request
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inquiryService.createInquiry(memberUuid, request));
    }

    /** 내 문의 목록 (로그인 필요) */
    @Operation(summary = "내 문의 목록")
    @GetMapping("/mine")
    public ResponseEntity<Page<InquiryResponse>> getMyInquiries(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        return ResponseEntity.ok(inquiryService.getMyInquiries(memberUuid, pageable));
    }

    /** 전체 문의 목록 (관리자 전용) */
    @Operation(summary = "전체 문의 목록 (관리자)")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<InquiryResponse>> getAllInquiries(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(inquiryService.getAllInquiries(pageable));
    }

    /** 답변 등록 (관리자 전용) */
    @Operation(summary = "문의 답변 등록 (관리자)")
    @PostMapping("/{inquiryId}/answer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InquiryResponse> createAnswer(
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryAnswerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inquiryService.createAnswer(inquiryId, request));
    }
}