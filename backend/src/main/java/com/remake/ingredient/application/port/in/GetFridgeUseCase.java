package com.remake.ingredient.application.port.in;

import java.util.List;

import com.remake.ingredient.domain.FridgeIngredient;
import com.remake.ingredient.domain.FridgeSummary;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.StorageLocation;

/** 냉장고 화면 조회. 진행 중(ACTIVE)인 재료만 다룬다. */
public interface GetFridgeUseCase {

	/** 필터는 모두 선택이다. 소비기한이 빠른 순(같으면 id 순)으로 돌려주고, 없으면 빈 목록이다. */
	List<FridgeIngredient> getFridgeIngredients(Long userId, StorageLocation storage,
			IngredientCategoryCode categoryCode, String keyword);

	/** 필터 없이 사용자의 모든 ACTIVE 재료를 센다. 없으면 모두 0이다. */
	FridgeSummary getFridgeSummary(Long userId);
}
