package com.remake.ingredient.domain;

/**
 * 재료 사전 한 항목(기준 데이터). 서버가 재료명으로 카테고리·보관 일수를 정할 때 쓴다.
 * shelfLifeDays가 null이면 카테고리 기본 보관 일수를 쓴다.
 */
public record IngredientCatalogItem(
		Long id,
		Long categoryId,
		String name,
		Integer shelfLifeDays,
		boolean commonExclusion) {

	/** 이 항목의 보관 일수. 항목 값이 없으면 카테고리 기본값 */
	public int effectiveShelfLifeDays(IngredientCategory category) {
		return shelfLifeDays != null ? shelfLifeDays : category.defaultShelfLifeDays();
	}
}
