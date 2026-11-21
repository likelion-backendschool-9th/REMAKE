package com.remake.ingredient.domain;

/** 재료 사전 검색 결과 한 줄. 사전 항목에 카테고리 code와, 항목 값이 없으면 카테고리 기본값으로 채운 보관 일수를 붙였다. */
public record IngredientCatalogEntry(IngredientCatalogItem item, IngredientCategoryCode categoryCode,
		int shelfLifeDays) {
}
