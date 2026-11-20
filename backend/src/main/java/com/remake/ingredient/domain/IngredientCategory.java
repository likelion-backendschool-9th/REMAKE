package com.remake.ingredient.domain;

/**
 * 재료 카테고리(기준 데이터). 재료 추가 화면의 "재료 종류"와, 소비기한·보관 위치·단위 기본값을 정한다.
 * 도메인 객체에는 Spring·JPA 어노테이션을 붙이지 않는다.
 */
public record IngredientCategory(
		Long id,
		IngredientCategoryCode code,
		String name,
		String imageUrl,
		StorageLocation defaultStorage,
		IngredientUnit defaultUnit,
		int defaultShelfLifeDays) {
}
