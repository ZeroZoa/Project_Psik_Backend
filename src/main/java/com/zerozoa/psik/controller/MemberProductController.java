package com.zerozoa.psik.controller;

import com.zerozoa.psik.dto.contents.ProductDto;
import com.zerozoa.psik.global.util.SecurityUtils;
import com.zerozoa.psik.service.MemberProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

//Member가 구매한 MemberProduct 관련 API 컨트롤러
@Tag(name = "MemberProduct API", description = "샀어요 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class MemberProductController {

    private final MemberProductService memberProductService;

    @Operation(summary = "샀어요 등록")
    @PostMapping("/{productId}/own")
    public ResponseEntity<Map<String, Object>> markAsOwned(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable Long productId
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        long count = memberProductService.markAsOwned(memberUuid, productId);
        return ResponseEntity.ok(Map.of("owned", true, "count", count));
    }

    @Operation(summary = "샀어요 여부 조회")
    @GetMapping("/{productId}/own")
    public ResponseEntity<Map<String, Object>> getOwned(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable Long productId
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        boolean owned = memberProductService.isOwned(memberUuid, productId);
        long count = memberProductService.countByProduct(productId);
        return ResponseEntity.ok(Map.of("owned", owned, "count", count));
    }

    @Operation(summary = "내가 샀어요 누른 제품 목록")
    @GetMapping("/me/owned")
    public ResponseEntity<List<ProductDto>> getMyOwnedProducts(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        return ResponseEntity.ok(memberProductService.getOwnedProducts(memberUuid));
    }

    @Operation(summary = "제품 검색", description = "이름/브랜드로 제품을 검색합니다. (다이어리 화장품 선택용)")
    @GetMapping("/search")
    public ResponseEntity<Page<ProductDto>> searchProducts(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(memberProductService.searchProducts(keyword, pageable));
    }
}