package com.remake.ingredient.application.port.in;

import java.util.List;

import com.remake.ingredient.domain.IngredientCategory;

/** 재료 카테고리 조회. Controller와 다른 기능(영수증 등)은 이 인터페이스로만 카테고리를 읽는다. */
public interface GetIngredientCategoriesUseCase {

	/** 전체 카테고리, id 순 */
	List<IngredientCategory> getIngredientCategories();

	/** 카테고리 하나. 없으면 BusinessException(INGREDIENT_CATEGORY_NOT_FOUND) */
	IngredientCategory getIngredientCategory(Long categoryId);
}
