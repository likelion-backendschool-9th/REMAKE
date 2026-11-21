package com.remake.ingredient.adapter.in.web;

import com.remake.ingredient.domain.IngredientCatalogEntry;
import com.remake.ingredient.domain.IngredientCategoryCode;

/** 재료 사전 한 항목. shelfLifeDays는 Service가 항목 값이 없으면 카테고리 기본값으로 채워 둔 값이다. */
public record IngredientCatalogResponse(
		Long id,
		String name,
		IngredientCategoryCode categoryCode,
		int shelfLifeDays,
		boolean isCommonExclusion) {

	public static IngredientCatalogResponse from(IngredientCatalogEntry entry) {
		return new IngredientCatalogResponse(entry.item().id(), entry.item().name(), entry.categoryCode(),
				entry.shelfLifeDays(), entry.item().commonExclusion());
	}
}
