package com.zerozoa.skinner.repository.contents;

import com.zerozoa.skinner.domain.contents.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Long>, IngredientRepositoryCustom {
}