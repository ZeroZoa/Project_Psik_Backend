package com.zerozoa.psik.repository.contents;

import com.zerozoa.psik.domain.contents.Ingredient;
import com.zerozoa.psik.domain.member.SkinConcern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


/**
 * 성분 리포지토리
 * 동적 검색은 IngredientRepositoryCustom (QueryDSL) 구현체에서 처리
 */
public interface IngredientRepository extends JpaRepository<Ingredient, Long>, IngredientRepositoryCustom {
    // 피부 고민 목록에 해당하는 성분 조회 — 추천 로직에서 사용
    @Query("SELECT DISTINCT i FROM Ingredient i JOIN i.skinConcerns sc WHERE sc IN :concerns")
    List<Ingredient> findBySkinConcernsIn(@Param("concerns") List<SkinConcern> concerns);
}