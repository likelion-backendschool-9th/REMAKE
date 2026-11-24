package com.remake.ingredient.application.port.in;

import java.util.List;

import com.remake.ingredient.domain.ExpiryStatus;
import com.remake.ingredient.domain.FridgeIngredient;
import com.remake.ingredient.domain.FridgeSummary;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.StorageLocation;

/** 냉장고 화면 조회. */
public interface GetFridgeUseCase {

	/**
	 * 필터는 모두 선택이고 함께 쓰면 모두 만족하는 재료만 돌려준다. status는 오늘 기준 소비기한 상태다.
	 * 소비기한이 빠른 순(같으면 id 순)이고, 없으면 빈 목록이다.
	 */
	List<FridgeIngredient> getFridgeIngredients(Long userId, ExpiryStatus status, StorageLocation storage,
			IngredientCategoryCode categoryCode);

	/** 필터 없이 사용자의 모든 재료를 센다. 없으면 모두 0이다. */
	FridgeSummary getFridgeSummary(Long userId);
}
