package com.zerozoa.psik.repository.contents;

import com.zerozoa.psik.domain.contents.Ingredient;
import com.zerozoa.psik.domain.member.SkinConcern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IngredientRepository extends JpaRepository<Ingredient, Long>, IngredientRepositoryCustom {
    @Query("SELECT DISTINCT i FROM Ingredient i JOIN i.skinConcerns sc WHERE sc IN :concerns")
    List<Ingredient> findBySkinConcernsIn(@Param("concerns") List<SkinConcern> concerns);
}