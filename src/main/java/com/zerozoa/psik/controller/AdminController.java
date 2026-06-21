package com.zerozoa.psik.controller;

import com.zerozoa.psik.dto.admin.IngredientCreateRequest;
import com.zerozoa.psik.dto.admin.IngredientUpdateRequest;
import com.zerozoa.psik.dto.admin.ProductCreateRequest;
import com.zerozoa.psik.dto.admin.ProductUpdateRequest;
import com.zerozoa.psik.dto.contents.IngredientDetailResponse;
import com.zerozoa.psik.dto.contents.ProductDto;
import com.zerozoa.psik.service.AdminService;
import com.zerozoa.psik.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@Tag(name = "Admin API", description = "관리자 전용 성분/제품 CRUD API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")  // 클래스 레벨 — 모든 엔드포인트 ADMIN 필수
public class AdminController {

    private final AdminService adminService;
    private final FileStorageService fileStorageService;

    // ──────────────── Ingredient ────────────────

    /**
     * 성분 생성 (관리자 전용)
     * @param request 성분 생성 요청 DTO
     * @return 201 Created - 생성된 성분 상세 정보
     * @see AdminService#createIngredient(IngredientCreateRequest)
     */
    @Operation(summary = "성분 생성")
    @PostMapping("/ingredients")
    public ResponseEntity<IngredientDetailResponse> createIngredient(
            @RequestBody @Valid IngredientCreateRequest request
    ) {
        log.info("[Admin] 성분 생성 요청");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createIngredient(request));
    }

    /**
     * 성분 수정 (관리자 전용)
     * @param id 수정할 성분의 ID
     * @param request 성분 수정 요청 DTO
     * @return 200 OK - 수정된 성분 상세 정보
     * @see AdminService#updateIngredient(Long, IngredientUpdateRequest)
     */
    @Operation(summary = "성분 수정")
    @PutMapping("/ingredients/{id}")
    public ResponseEntity<IngredientDetailResponse> updateIngredient(
            @PathVariable Long id,
            @RequestBody @Valid IngredientUpdateRequest request
    ) {
        log.info("[Admin] 성분 수정 요청 - id={}", id);
        return ResponseEntity.ok(adminService.updateIngredient(id, request));
    }

    /**
     * 성분 삭제 (관리자 전용)
     * @param id 삭제할 성분의 ID
     * @return 204 No Content
     * @see AdminService#deleteIngredient(Long)
     */
    @Operation(summary = "성분 삭제")
    @DeleteMapping("/ingredients/{id}")
    public ResponseEntity<Void> deleteIngredient(@PathVariable Long id) {
        log.info("[Admin] 성분 삭제 요청 - id={}", id);
        adminService.deleteIngredient(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 성분-제품 연결 (관리자 전용)
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
        log.info("[Admin] 성분-제품 연결 요청 - ingredientId={}, productId={}", ingredientId, productId);
        adminService.linkProduct(ingredientId, productId);
        return ResponseEntity.ok().build();
    }

    /**
     * 성분-제품 연결 해제 (관리자 전용)
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
        log.info("[Admin] 성분-제품 연결 해제 요청 - ingredientId={}, productId={}", ingredientId, productId);
        adminService.unlinkProduct(ingredientId, productId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 전체 성분 임베딩 일괄 생성 (관리자 전용)
     * 최초 배포 시 또는 임베딩 모델 변경 시 1회 실행
     */
    @Operation(summary = "전체 성분 임베딩 생성")
    @PostMapping("/ingredients/embed-all")
    public ResponseEntity<Map<String, String>> embedAll() {
        log.info("[Admin] 전체 임베딩 요청");
        String result = adminService.embedAll();
        return ResponseEntity.ok(Map.of("result", result));
    }

    // ──────────────── Product ────────────────

    /**
     * 전체 제품 목록 조회 (관리자 전용 - 성분 연결용)
     * @param pageable 페이지네이션 정보 (기본값: size=20, id 오름차순)
     * @return 200 OK - 전체 제품 목록 (페이지)
     * @see AdminService#getAllProducts(Pageable)
     */
    @Operation(summary = "전체 제품 목록 조회 (성분 연결용)")
    @GetMapping("/products")
    public ResponseEntity<Page<ProductDto>> getAllProducts(
            @PageableDefault(size = 200, sort = "id") Pageable pageable  // 20 → 200
    ) {
        return ResponseEntity.ok(adminService.getAllProducts(pageable));
    }

    /**
     * 제품 생성 (관리자 전용)
     * @param request 제품 생성 요청 DTO
     * @return 201 Created - 생성된 제품 정보
     * @see AdminService#createProduct(ProductCreateRequest)
     */
    @Operation(summary = "제품 생성")
    @PostMapping("/products")
    public ResponseEntity<ProductDto> createProduct(
            @RequestBody @Valid ProductCreateRequest request
    ) {
        log.info("[Admin] 제품 생성 요청");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createProduct(request));
    }

    /**
     * 제품 이미지 업로드 (관리자 전용)
     * Content-Type: multipart/form-data
     * @param image 업로드할 이미지 파일
     * @return 200 OK - {"imageUrl": "업로드된 이미지 URL"}
     */
    @Operation(summary = "제품 이미지 업로드")
    @PostMapping(value = "/products/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadProductImage(
            @RequestPart("image") MultipartFile image
    ) {
        log.info("[Admin] 제품 이미지 업로드 요청");
        String url = fileStorageService.store(image, "products");
        return ResponseEntity.ok(Map.of("imageUrl", url));
    }

    /**
     * 제품 수정 (관리자 전용)
     * @param id 수정할 제품의 ID
     * @param request 제품 수정 요청 DTO
     * @return 200 OK - 수정된 제품 정보
     * @see AdminService#updateProduct(Long, ProductUpdateRequest)
     */
    @Operation(summary = "제품 수정")
    @PutMapping("/products/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable Long id,
            @RequestBody @Valid ProductUpdateRequest request
    ) {
        log.info("[Admin] 제품 수정 요청 - id={}", id);
        return ResponseEntity.ok(adminService.updateProduct(id, request));
    }

    /**
     * 제품 삭제 (관리자 전용)
     * @param id 삭제할 제품의 ID
     * @return 204 No Content
     * @see AdminService#deleteProduct(Long)
     */
    @Operation(summary = "제품 삭제")
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.warn("[Admin] 제품 삭제 요청 - id={}", id);
        adminService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}