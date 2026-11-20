package com.remake.ingredient.application.port.in;

import java.util.List;

import com.remake.ingredient.domain.IngredientCategory;
import com.remake.ingredient.domain.IngredientCategoryCode;

/** 재료 카테고리 조회. Controller와 다른 기능(재료 등록, 영수증 등)은 이 인터페이스로만 카테고리를 읽는다. */
public interface GetIngredientCategoriesUseCase {

	/** 전체 카테고리, id 순 */
	List<IngredientCategory> getIngredientCategories();

	/**
	 * code로 카테고리 하나. API가 받는 categoryCode를 확인하고 기본값을 가져올 때 쓴다.
	 * 요청 DTO의 categoryCode를 IngredientCategoryCode로 받으면, 없는 code는 여기 오기 전에 400(INVALID_INPUT)으로 막힌다.
	 * 그래서 BusinessException(INGREDIENT_CATEGORY_NOT_FOUND)은 enum과 DB 기준 데이터가 어긋났을 때만 나온다.
	 */
	IngredientCategory getIngredientCategoryByCode(IngredientCategoryCode code);

	/** id로 카테고리 하나. DB에 저장된 category_id를 다시 읽을 때 쓴다. 없으면 BusinessException(INGREDIENT_CATEGORY_NOT_FOUND) */
	IngredientCategory getIngredientCategory(Long categoryId);
}
