package com.zerozoa.psik.controller;

import com.zerozoa.psik.dto.contents.ProductDto;
import com.zerozoa.psik.global.util.SecurityUtils;
import com.zerozoa.psik.service.MemberProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;


@Slf4j
@Tag(name = "MemberProduct API", description = "샀어요 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class MemberProductController {

    private final MemberProductService memberProductService;


    /**
     * 샀어요 등록
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입)
     * @param productId 샀어요에 등록할 제품의 ID
     * @return 200 OK - owned(true), count(샀어요 총 수)
     * @see MemberProductService#markAsOwned(UUID, Long)
     */
    @Operation(summary = "샀어요 등록")
    @PostMapping("/{productId}/own")
    public ResponseEntity<Map<String, Object>> markAsOwned(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable Long productId
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        log.info("[API] 샀어요 등록 요청 - memberUuid={}, productId={}", memberUuid, productId);
        long count = memberProductService.markAsOwned(memberUuid, productId);
        return ResponseEntity.ok(Map.of("owned", true, "count", count));
    }

    /**
     * 샀어요 여부 조회
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입)
     * @param productId 샀어요에 여부를 조회할 제품의 ID
     * @return 200 OK - owned(샀어요 여부), count(샀어요 총 수)
     * @see MemberProductService#getOwnStatus(UUID, Long)
     */
    @Operation(summary = "샀어요 여부 조회")
    @GetMapping("/{productId}/own")
    public ResponseEntity<Map<String, Object>> getOwned(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable Long productId
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        return ResponseEntity.ok(memberProductService.getOwnStatus(memberUuid, productId));
    }

    /**
     * 내가 샀어요 누른 제품 목록 조회
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입)
     * @return 200 OK - 내가 샀어요 등록한 제품 목록 List
     * @see MemberProductService#getOwnedProducts(UUID)
     */
    @Operation(summary = "내가 샀어요 누른 제품 목록")
    @GetMapping("/me/owned")
    public ResponseEntity<List<ProductDto>> getMyOwnedProducts(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        return ResponseEntity.ok(memberProductService.getOwnedProducts(memberUuid));
    }

    /**
     * 제품 검색 (이름/브랜드, 다이어리 화장품 선택용)
     * @param keyword 검색할 키워드 (null이면 전체 조회)
     * @param pageable 페이지네이션 정보 (기본값: size=20, id 오름차순)
     * @return 200 OK - 검색된 제품 목록 (페이지)
     * @see MemberProductService#searchProducts(String, Pageable)
     */
    @Operation(summary = "제품 검색", description = "이름/브랜드로 제품을 검색합니다. (다이어리 화장품 선택용)")
    @GetMapping("/search")
    public ResponseEntity<Page<ProductDto>> searchProducts(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(memberProductService.searchProducts(keyword, pageable));
    }
}