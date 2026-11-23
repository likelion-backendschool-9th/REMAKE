package com.remake.recipe.domain;

/**
 * 레시피 재료 단위 14개(domain-rules "Units"). DB와 JSON에는 이름 그대로 들어간다.
 * 냉장고 재료 단위(IngredientUnit 9개)는 이 중 냉장고에서 쓰는 것만 모은 것이다.
 */
public enum RecipeUnit {
	G,
	KG,
	ML,
	L,
	TBSP,
	TSP,
	CUP,
	EA,
	PACK,
	BUNCH,
	PIECE,
	SHEET,
	PINCH,
	TO_TASTE
}
