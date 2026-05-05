package com.zerozoa.psik.repository.contents;

import com.zerozoa.psik.domain.contents.Ingredient;
import com.zerozoa.psik.domain.contents.IngredientType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 성분(Ingredient) 쿼리를 처리하기 위한 Custom Interface
 *Spring Data JPA가 자동으로 생성해주지 못하는 동적 쿼리, 복잡한 조인을 정의
 *QueryDSL을 사용하는 구현체(Impl)가 이 인터페이스를 상속받아 로직 구현
 */
public interface IngredientRepositoryCustom {
    /**
     * 성분 검색 및 필터링
     * @param keyword 검색어 (이름, 설명 포함) - null이면 검색 안 함
     * @param type 성분 타입 (일반, 의약품 등) - null이면 전체 조회
     * @param pageable 페이징 및 정렬 정보
     * @return 검색된 성분 목록 (페이징 포함)
     */
    Page<Ingredient> search(String keyword, IngredientType type, Pageable pageable);
}
