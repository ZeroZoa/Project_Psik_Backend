package com.zerozoa.skinner.controller;

import com.zerozoa.skinner.dto.contents.ProductDto;
import com.zerozoa.skinner.global.util.SecurityUtils;
import com.zerozoa.skinner.service.MemberProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;


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
        long count = memberProductService.markAsOwned(memberUuid, productId);
        return ResponseEntity.ok(Map.of("owned", true, "count", count));
    }

    /**
     * 샀어요 여부 조회
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입)
     * @param productId 샀어요에 여부를 조회할 제품의 ID
     * @return 200 OK - owned(샀어요 여부), count(샀어요 총 수)
     * @see MemberProductService#isOwned(UUID, Long)
     * @see MemberProductService#countByProduct(Long)
     */
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
}