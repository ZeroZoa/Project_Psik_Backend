package com.zerozoa.skinner.service;

import com.zerozoa.skinner.domain.contents.Ingredient;
import com.zerozoa.skinner.domain.contents.Product;
import com.zerozoa.skinner.dto.admin.IngredientCreateRequest;
import com.zerozoa.skinner.dto.admin.IngredientUpdateRequest;
import com.zerozoa.skinner.dto.admin.ProductCreateRequest;
import com.zerozoa.skinner.dto.admin.ProductUpdateRequest;
import com.zerozoa.skinner.dto.contents.IngredientDetailResponse;
import com.zerozoa.skinner.dto.contents.ProductDto;
import com.zerozoa.skinner.global.exception.BusinessException;
import com.zerozoa.skinner.global.exception.ErrorCode;
import com.zerozoa.skinner.repository.contents.IngredientRepository;
import com.zerozoa.skinner.repository.contents.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final IngredientRepository ingredientRepository;
    private final ProductRepository productRepository;

    // ──────────────── Ingredient ────────────────

    /**
     * 성분 생성
     */
    @Transactional
    public IngredientDetailResponse createIngredient(IngredientCreateRequest request) {
        Ingredient ingredient = Ingredient.builder()
                .name(request.name())
                .type(request.type())
                .effectSummary(request.effectSummary())
                .description(request.description())
                .build();

        request.effects().forEach(ingredient::addEffect);
        request.cautions().forEach(ingredient::addCaution);
        request.skinConcerns().forEach(ingredient::addSkinConcern);

        Ingredient saved = ingredientRepository.save(ingredient);
        log.info("[Admin] 성분 생성 완료 - id={}, name={}", saved.getId(), saved.getName());
        return IngredientDetailResponse.from(saved);
    }

    /**
     * 성분 수정
     */
    @Transactional
    public IngredientDetailResponse updateIngredient(Long id, IngredientUpdateRequest request) {
        Ingredient ingredient = getIngredientOrThrow(id);

        // 기본 필드 업데이트
        ingredient.update(
                request.name(),
                request.type(),
                request.effectSummary(),
                request.description()
        );

        // 컬렉션 전체 교체 (clear → addAll)
        ingredient.clearEffects();
        request.effects().forEach(ingredient::addEffect);

        ingredient.clearCautions();
        request.cautions().forEach(ingredient::addCaution);

        ingredient.clearSkinConcerns();
        request.skinConcerns().forEach(ingredient::addSkinConcern);

        log.info("[Admin] 성분 수정 완료 - id={}", id);
        return IngredientDetailResponse.from(ingredient);
    }

    /**
     * 성분 삭제
     */
    @Transactional
    public void deleteIngredient(Long id) {
        Ingredient ingredient = getIngredientOrThrow(id);
        ingredient.getProducts().forEach(p -> p.getIngredients().remove(ingredient));
        ingredientRepository.delete(ingredient);
        log.info("[Admin] 성분 삭제 완료 - id={}", id);
    }

    /**
     * 성분에 제품 연결
     */
    @Transactional
    public void linkProduct(Long ingredientId, Long productId) {
        Ingredient ingredient = getIngredientOrThrow(ingredientId);
        Product product = getProductOrThrow(productId);
        ingredient.addProduct(product);
        log.info("[Admin] 성분-제품 연결 완료 - ingredientId={}, productId={}", ingredientId, productId);
    }

    /**
     * 성분에서 제품 연결 해제
     */
    @Transactional
    public void unlinkProduct(Long ingredientId, Long productId) {
        Ingredient ingredient = getIngredientOrThrow(ingredientId);
        Product product = getProductOrThrow(productId);
        ingredient.getProducts().remove(product);
        product.getIngredients().remove(ingredient);
        log.info("[Admin] 성분-제품 연결 해제 완료 - ingredientId={}, productId={}", ingredientId, productId);
    }

    // ──────────────── Product ────────────────

    /**
     * 전체 제품 목록 조회 (성분 연결용)
     */
    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductDto::from)
                .toList();
    }

    /**
     * 제품 생성
     */
    @Transactional
    public ProductDto createProduct(ProductCreateRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .brand(request.brand())
                .price(request.price())
                .description(request.description())
                .link(request.link())
                .imageUrl(request.imageUrl())
                .build();

        Product saved = productRepository.save(product);
        log.info("[Admin] 제품 생성 완료 - id={}, name={}", saved.getId(), saved.getName());
        return ProductDto.from(saved);
    }

    /**
     * 제품 수정
     */
    @Transactional
    public ProductDto updateProduct(Long id, ProductUpdateRequest request) {
        Product product = getProductOrThrow(id);
        product.update(
                request.name(),
                request.brand(),
                request.price(),
                request.description(),
                request.link(),
                request.imageUrl()
        );
        log.info("[Admin] 제품 수정 완료 - id={}", id);
        return ProductDto.from(product);
    }

    /**
     * 제품 삭제
     */
    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductOrThrow(id);
        // 양방향 관계 정리 후 삭제
        product.getIngredients().forEach(i -> i.getProducts().remove(product));
        productRepository.delete(product);
        log.info("[Admin] 제품 삭제 완료 - id={}", id);
    }

    // ──────────────── 내부 헬퍼 ────────────────

    private Ingredient getIngredientOrThrow(Long id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));
    }

    private Product getProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}