package com.remake.ingredient.domain;

/**
 * 재료 카테고리 code 12개. ingredient_categories.code와 같고, API의 categoryCode도 이 이름을 쓴다.
 * 순서는 V1 마이그레이션의 id 순서와 같다. 카테고리를 추가하면 마이그레이션과 함께 여기도 추가한다.
 */
public enum IngredientCategoryCode {
	VEGETABLE,
	FRUIT,
	MEAT,
	SEAFOOD,
	DAIRY_EGG,
	GRAIN,
	BEAN_TOFU,
	PROCESSED,
	SIDE_DISH,
	SEASONING,
	BEVERAGE,
	ETC
}
