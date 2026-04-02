package com.zerozoa.skinner.service;

import com.zerozoa.skinner.domain.contents.Ingredient;
import com.zerozoa.skinner.domain.contents.IngredientType;
import com.zerozoa.skinner.domain.member.SkinConcern;
import com.zerozoa.skinner.dto.contents.IngredientDetailResponse;
import com.zerozoa.skinner.dto.contents.IngredientResponse;
import com.zerozoa.skinner.global.exception.BusinessException;
import com.zerozoa.skinner.global.exception.ErrorCode;
import com.zerozoa.skinner.repository.contents.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

//Ingredient 관련 비지니스 로직을 담당하는 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    /**
     * 성분 목록 조회
     * 동작 원리:
     * 1. Repository(QueryDSL)를 통해 페이징된 엔티티 목록(Page<Ingredient>)을 조회
     * 2. 조회된 엔티티를 바로 DTO(IngredientResponse)로 변환
     * @param keyword 검색어 (성분명, 효능 등)
     * @param type    성분 타입 필터 (일반, 의약품 등)
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
     * @param id 성분 PK
     * @return 상세 정보 DTO (효능, 주의사항, 추천 제품 포함)
     */
    public IngredientDetailResponse getIngredientDetail(Long id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                // 에러 코드(INGREDIENT_NOT_FOUND)가 없다면 MEMBER_NOT_FOUND 등을 임시로 쓰거나 새로 추가 필요
                .orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));

        return IngredientDetailResponse.from(ingredient);
    }

    /**
     * 사용자별 SkinConcern 상세 조회
     * @param concerns
     * @return SkinConcern에 해당하는 Ingredient추천
     */
    public List<IngredientResponse> getRecommendedIngredients(List<SkinConcern> concerns) {
        return ingredientRepository.findBySkinConcernsIn(concerns)
                .stream()
                .map(IngredientResponse::from)
                .toList();
    }
}