package com.remake.ingredient.application.port.out;

import java.util.List;
import java.util.Optional;

import com.remake.ingredient.domain.IngredientCategory;

/** 카테고리 저장소 읽기. ingredient 기능의 Service만 쓴다(다른 기능은 UseCase를 쓴다). */
public interface LoadIngredientCategoryPort {

	List<IngredientCategory> loadAll();

	Optional<IngredientCategory> loadById(Long categoryId);
}
