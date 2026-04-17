package com.zerozoa.skinner.controller;

import com.zerozoa.skinner.dto.admin.IngredientCreateRequest;
import com.zerozoa.skinner.dto.admin.IngredientUpdateRequest;
import com.zerozoa.skinner.dto.admin.ProductCreateRequest;
import com.zerozoa.skinner.dto.admin.ProductUpdateRequest;
import com.zerozoa.skinner.dto.contents.IngredientDetailResponse;
import com.zerozoa.skinner.dto.contents.ProductDto;
import com.zerozoa.skinner.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Admin API", description = "관리자 전용 성분/제품 CRUD API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")  // 클래스 레벨 — 모든 엔드포인트 ADMIN 필수
public class AdminController {

    private final AdminService adminService;

    // ───────────────────── Ingredient ─────────────────────

    /**
     * 성분 생성
     * @param request 성분 생성 요청
     * @return 201 Created
     * @see AdminService#createIngredient(IngredientCreateRequest)
     */
    @Operation(summary = "성분 생성")
    @PostMapping("/ingredients")
    public ResponseEntity<IngredientDetailResponse> createIngredient(
            @RequestBody @Valid IngredientCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createIngredient(request));
    }

    /**
     * 성분 수정
     * @param ingredientId 수정할 성분의 ID
     * @param request 성분 수정 요청
     * @return 200 OK
     * @see AdminService#updateIngredient(Long, IngredientUpdateRequest) 
     */
    @Operation(summary = "성분 수정")
    @PutMapping("/ingredients/{ingredientId}")
    public ResponseEntity<IngredientDetailResponse> updateIngredient(
            @PathVariable Long ingredientId,
            @RequestBody @Valid IngredientUpdateRequest request
    ) {
        return ResponseEntity.ok(adminService.updateIngredient(ingredientId, request));
    }


    /**
     * 성분 삭제
     * @param ingredientId 삭제할 성분의 ID
     * @return 204 No Content
     * @see AdminService#deleteIngredient(Long)
     */
    @Operation(summary = "성분 삭제")
    @DeleteMapping("/ingredients/{ingredientId}")
    public ResponseEntity<Void> deleteIngredient(@PathVariable Long ingredientId) {
        log.info("[Admin] 성분 삭제 요청 - id={}", ingredientId);
        adminService.deleteIngredient(ingredientId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 성분-제품 연결
     * @param ingredientId 연결할 성분의 ID
     * @param productId 연결할 제품의 ID
     * @return 200 OK
     * @see AdminService#linkProduct(Long, Long)
     */
    @Operation(summary = "성분-제품 연결")
    @PostMapping("/ingredients/{ingredientId}/products/{productId}")
    public ResponseEntity<Void> linkProduct(
            @PathVariable Long ingredientId,
            @PathVariable Long productId
    ) {
        adminService.linkProduct(ingredientId, productId);
        return ResponseEntity.ok().build();
    }

    /**
     * 성분-제품 연결 해제
     * @param ingredientId 연결 해제할 성분의 ID
     * @param productId 연결 해제할 제품의 ID
     * @return 204 No Content
     * @see AdminService#unlinkProduct(Long, Long)
     */
    @Operation(summary = "성분-제품 연결 해제")
    @DeleteMapping("/ingredients/{ingredientId}/products/{productId}")
    public ResponseEntity<Void> unlinkProduct(
            @PathVariable Long ingredientId,
            @PathVariable Long productId
    ) {
        adminService.unlinkProduct(ingredientId, productId);
        return ResponseEntity.noContent().build();
    }

    // ───────────────────── Product ─────────────────────

    /**
     * 전체 제품 목록 조회
     * @param pageable 페이징 정보 (페이지 번호, 사이즈, 정렬)
     * @return 200 OK
     * @see AdminService#getAllProducts(Pageable)
     */
    @Operation(summary = "전체 제품 목록 조회 (성분 연결용)")
    @GetMapping("/products")
    public ResponseEntity<Page<ProductDto>> getAllProducts(
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.getAllProducts(pageable));
    }

    /**
     * 제품 생성
     * @param request 제품 생성 요청
     * @return 201 Created
     * @see AdminService#createProduct(ProductCreateRequest)
     */
    @Operation(summary = "제품 생성")
    @PostMapping("/products")
    public ResponseEntity<ProductDto> createProduct(
            @RequestBody @Valid ProductCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createProduct(request));
    }

    /**
     * 제품 수정
     * @param id 수정할 제품의 ID
     * @param request 제품 수정 요청
     * @return 200 OK
     * @see AdminService#updateProduct(Long, ProductUpdateRequest)
     */
    @Operation(summary = "제품 수정")
    @PutMapping("/products/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable Long id,
            @RequestBody @Valid ProductUpdateRequest request
    ) {
        return ResponseEntity.ok(adminService.updateProduct(id, request));
    }


    /**
     * 제품 삭제
     * @param id 삭제할 제품의 ID
     * @return 204 No Content
     * @see AdminService#deleteProduct(Long)
     */
    @Operation(summary = "제품 삭제")
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        adminService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}