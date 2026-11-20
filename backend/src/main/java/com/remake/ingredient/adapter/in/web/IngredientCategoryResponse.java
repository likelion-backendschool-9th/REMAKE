package com.remake.ingredient.adapter.in.web;

import com.remake.ingredient.domain.IngredientCategory;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.Storage;

/**
 * 재료 카테고리 응답. 도메인 객체·JPA 엔티티를 그대로 내보내지 않고 Response DTO로 바꾼다.
 * 감싸는 클래스(ApiResponse 등) 없이 이 모양 그대로 응답한다. enum은 이름 그대로(FRIDGE) 나간다.
 */
public record IngredientCategoryResponse(
		Long id,
		String code,
		String name,
		String imageUrl,
		Storage defaultStorage,
		IngredientUnit defaultUnit,
		int defaultShelfLifeDays) {

	public static IngredientCategoryResponse from(IngredientCategory category) {
		return new IngredientCategoryResponse(category.id(), category.code(), category.name(), category.imageUrl(),
				category.defaultStorage(), category.defaultUnit(), category.defaultShelfLifeDays());
	}
}
