package com.remake.ingredient.domain;

/**
 * 재료 카테고리 code 12개. ingredient_categories.code와 같고, API의 categoryCode도 이 이름을 쓴다.
 * 화면에 보이는 순서는 sort_order(ERD 부록 A)이고 이 선언 순서와 무관하다. 카테고리를 추가하면 마이그레이션과 함께 여기도 추가한다.
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
