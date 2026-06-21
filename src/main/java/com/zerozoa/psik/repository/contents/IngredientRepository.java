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

    /**
     * 벡터 유사도 기반 성분 조회
     * pgvector cosine distance로 유사 성분 조회
     * threshold: 유사도 하한선 (0~1, 높을수록 엄격) — 관련 없는 성분 차단
     * limit: 반환할 최대 성분 수 — LLM 컨텍스트 토큰 비용 제어
     */
    @Query(value = """
    SELECT * FROM ingredient
    WHERE embedding IS NOT NULL
      AND 1 - (embedding <=> CAST(:embedding AS vector)) > :threshold
    ORDER BY embedding <=> CAST(:embedding AS vector)
    LIMIT :limit
    """, nativeQuery = true)
    List<Ingredient> findSimilarIngredients(
            @Param("embedding") String embedding,
            @Param("threshold") double threshold,
            @Param("limit") int limit
    );
}