package com.zerozoa.psik.service;

import com.zerozoa.psik.domain.contents.Ingredient;
import com.zerozoa.psik.domain.contents.IngredientType;
import com.zerozoa.psik.domain.member.SkinConcern;
import com.zerozoa.psik.dto.contents.IngredientDetailResponse;
import com.zerozoa.psik.dto.contents.IngredientResponse;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import com.zerozoa.psik.repository.contents.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    /**
     * 성분 목록 조회
     * @param keyword 검색어 (성분명, 효능 등)
     * @param type 성분 타입 필터 (일반, 의약품 등)
     * @param pageable 페이징 정보 (페이지 번호, 사이즈, 정렬)
     * @return 화면에 뿌려질 DTO 페이징 객체
     */
    public Page<IngredientResponse> getIngredients(String keyword, IngredientType type, Pageable pageable) {
        // QueryDSL로 동적 쿼리 실행
        Page<Ingredient> ingredients = ingredientRepository.search(keyword, type, pageable);
        // Entity -> DTO 변환
        return ingredients.map(IngredientResponse::from);
    }

    /**
     * 성분 상세 조회
     * @param id 상세 조회할 성분의 id
     * @throws BusinessException 성분을 찾을 수 없는 경우 {@link ErrorCode#INGREDIENT_NOT_FOUND}
     * @return 성분 상세 정보 DTO
     */
    public IngredientDetailResponse getIngredientDetail(Long id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));

        return IngredientDetailResponse.from(ingredient);
    }

    /**
     * 피부 고민별 추천 성분 목록 조회
     * @param concerns 조회할 피부 고민 목록
     * @return 피부고민에 해당하는 List<IngredientResponse>
     */
    public List<IngredientResponse> getRecommendedIngredients(List<SkinConcern> concerns) {
        return ingredientRepository.findBySkinConcernsIn(concerns)
                .stream()
                .map(IngredientResponse::from)
                .toList();
    }
}