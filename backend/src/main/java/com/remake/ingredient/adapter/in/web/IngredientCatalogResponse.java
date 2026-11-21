package com.remake.ingredient.adapter.in.web;

import com.remake.ingredient.domain.IngredientCatalogItem;
import com.remake.ingredient.domain.IngredientCategory;
import com.remake.ingredient.domain.IngredientCategoryCode;

/** 재료 사전 한 항목. shelfLifeDays는 항목 값이 없으면 카테고리 기본값으로 채워 준다. */
public record IngredientCatalogResponse(
		Long id,
		String name,
		IngredientCategoryCode categoryCode,
		int shelfLifeDays,
		boolean isCommonExclusion) {

	public static IngredientCatalogResponse from(IngredientCatalogItem item, IngredientCategory category) {
		return new IngredientCatalogResponse(item.id(), item.name(), category.code(),
				item.effectiveShelfLifeDays(category), item.commonExclusion());
	}
}
