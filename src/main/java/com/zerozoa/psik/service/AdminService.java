package com.zerozoa.psik.service;

import com.zerozoa.psik.domain.contents.Ingredient;
import com.zerozoa.psik.domain.contents.Product;
import com.zerozoa.psik.dto.admin.IngredientCreateRequest;
import com.zerozoa.psik.dto.admin.IngredientUpdateRequest;
import com.zerozoa.psik.dto.admin.ProductCreateRequest;
import com.zerozoa.psik.dto.admin.ProductUpdateRequest;
import com.zerozoa.psik.dto.contents.IngredientDetailResponse;
import com.zerozoa.psik.dto.contents.ProductDto;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import com.zerozoa.psik.repository.contents.IngredientRepository;
import com.zerozoa.psik.repository.contents.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final EmbeddingService embeddingService;

    // ───────────────────── Ingredient  ─────────────────────

    /**
     * 성분 생성
     * @param request 성분 생성 요청 DTO
     * @return IngredientDetailResponse
     */
    @Transactional
    public IngredientDetailResponse createIngredient(IngredientCreateRequest request) {
        Ingredient ingredient = Ingredient.builder()
                .name(request.name())
                .type(request.type())
                .effectSummary(request.effectSummary())
                .description(request.description())
                .build();

        //효과 add
        request.effects().forEach(ingredient::addEffect);
        //주의사항 add
        request.cautions().forEach(ingredient::addCaution);
        //피부고민 add
        request.skinConcerns().forEach(ingredient::addSkinConcern);

        Ingredient saved = ingredientRepository.save(ingredient);

        updateEmbedding(ingredient);
        log.info("[Admin] 성분 생성 완료 - id={}, name={}", saved.getId(), saved.getName());
        return IngredientDetailResponse.from(saved);
    }

    /**
     * 성분 수정
     * @param ingredientId 수정할 Ingredient의 ID
     * @param request 성분 수정 요청 DTO
     * @return IngredientDetailResponse
     */
    @Transactional
    public IngredientDetailResponse updateIngredient(Long ingredientId, IngredientUpdateRequest request) {
        Ingredient ingredient = getIngredient(ingredientId);

        // 기본 필드 업데이트
        ingredient.update(
                request.name(),
                request.type(),
                request.effectSummary(),
                request.description()
        );

        // 연관관계 전체 교체 (clear → addAll)

        //효과 clear 후 add
        ingredient.clearEffects();
        request.effects().forEach(ingredient::addEffect);

        //주의사항 clear 후 add
        ingredient.clearCautions();
        request.cautions().forEach(ingredient::addCaution);

        //피부고민 clear 후 add
        ingredient.clearSkinConcerns();
        request.skinConcerns().forEach(ingredient::addSkinConcern);

        updateEmbedding(ingredient);
        log.info("[Admin] 성분 수정 완료 - id={}", ingredientId);
        return IngredientDetailResponse.from(ingredient);
    }

    /**
     * 성분 삭제
     * @param ingredientId 수정할 Ingredient의 ID
     */
    @Transactional
    public void deleteIngredient(Long ingredientId) {
        //삭제할 성분 조회
        Ingredient ingredient = getIngredient(ingredientId);

        //삭제할 성분에 연관관계인 Product 제거
        ingredient.getProducts().forEach(p -> p.getIngredients().remove(ingredient));

        //성분 삭제
        ingredientRepository.delete(ingredient);
        log.info("[Admin] 성분 삭제 완료 - id={}", ingredientId);
    }

    /**
     * 성분과 제품 연결
     * @param ingredientId Ingredient(주인)의 ID
     * @param productId 성분에 link될 Product의 ID
     */
    @Transactional
    public void linkProduct(Long ingredientId, Long productId) {
        Ingredient ingredient = getIngredient(ingredientId);
        Product product = getProduct(productId);
        ingredient.addProduct(product);
        log.info("[Admin] 성분-제품 연결 완료 - ingredientId={}, productId={}", ingredientId, productId);
    }

    /**
     * 성분에서 제품 연결 해제
     * @param ingredientId Ingredient(주인)의 ID
     * @param productId 성분에 unlink될 Product의 ID
     */
    @Transactional
    public void unlinkProduct(Long ingredientId, Long productId) {
        Ingredient ingredient = getIngredient(ingredientId);
        Product product = getProduct(productId);
        ingredient.getProducts().remove(product);
        product.getIngredients().remove(ingredient);
        log.info("[Admin] 성분-제품 연결 해제 완료 - ingredientId={}, productId={}", ingredientId, productId);
    }

    // ───────────────────── Product  ─────────────────────

    /**
     * 전체 제품 목록 조회 (성분 연결용)
     * @param  pageable 페이징 정보 (페이지 번호, 사이즈, 정렬)
     * @return ProductDto Page
     *
     */
    public Page<ProductDto> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(ProductDto::from);
    }

    /**
     * 제품 생성
     * @param request 제품 생성 요청 DTO
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
     * @param productId 수정할 Product의 ID
     * @param request 제품 수정 요청 DTO
     */
    @Transactional
    public ProductDto updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = getProduct(productId);
        product.update(
                request.name(),
                request.brand(),
                request.price(),
                request.description(),
                request.link(),
                request.imageUrl()
        );
        log.info("[Admin] 제품 수정 완료 - id={}", productId);
        return ProductDto.from(product);
    }

    /**
     * 제품 삭제
     * @param productId 삭제할 Product의 ID
     */
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = getProduct(productId);
        // 성분관의 양방향 관계 정리 후 삭제
        product.getIngredients().forEach(i -> i.getProducts().remove(product));
        productRepository.delete(product);
        log.info("[Admin] 제품 삭제 완료 - id={}", productId);
    }

    // ───────────────────── 임베딩 ─────────────────────

    /**
     * 기존 성분 전체 임베딩 일괄 생성 — 최초 배포 시 또는 임베딩 모델 변경 시 1회 실행
     */
    @Transactional
    public String embedAll() {
        List<Ingredient> all = ingredientRepository.findAll();
        int success = 0;

        for (Ingredient ingredient : all) {
            try {
                String text = embeddingService.buildIngredientText(ingredient);
                ingredient.updateEmbedding(embeddingService.embed(text));
                success++;
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("[Admin] 임베딩 실패 — id={}, name={}", ingredient.getId(), ingredient.getName());
            }
        }

        log.info("[Admin] 전체 임베딩 완료 — {}/{}", success, all.size());
        return "완료: %d/%d".formatted(success, all.size());
    }

    /**
     * 성분 임베딩 생성 후 엔티티에 저장
     * 임베딩 API 실패 시 예외를 삼켜 성분 등록/수정 트랜잭션이 롤백되지 않도록 처리
     * 실패한 성분은 /api/admin/ingredients/embed-all 로 재실행
     */
    private void updateEmbedding(Ingredient ingredient) {
        try {
            String text = embeddingService.buildIngredientText(ingredient);
            ingredient.updateEmbedding(embeddingService.embed(text));
        } catch (Exception e) {
            log.warn("[Admin] 임베딩 생성 실패 — id={}, name={} | embed-all로 재실행 가능",
                    ingredient.getId(), ingredient.getName());
        }
    }

    // ───────────────────── 내부 헬퍼 ─────────────────────

    /**
     * 성분 찾기
     * @param ingredientId 찾을 성분의 ID
     * @throws BusinessException Ingredient가 존재하지 않는 경우 {@link ErrorCode#INGREDIENT_NOT_FOUND} 예외 발생
     * @return Ingredient
     */
    private Ingredient getIngredient(Long ingredientId) {
        return ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));
    }

    /**
     * 제품 찾기
     * @param productId 찾을 제품의 ID
     * @throws BusinessException Product가 존재하지 않는 경우 {@link ErrorCode#PRODUCT_NOT_FOUND} 예외 발생
     * @return Product
     */
    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}