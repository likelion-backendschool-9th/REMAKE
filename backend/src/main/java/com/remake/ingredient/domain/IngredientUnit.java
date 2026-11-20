package com.remake.ingredient.domain;

/** 냉장고 재료 단위 9개. 레시피 단위(14개)와는 다르다. DB와 JSON에는 이름 그대로(G) 들어간다. */
public enum IngredientUnit {
	G,
	KG,
	ML,
	L,
	EA,
	PACK,
	BUNCH,
	PIECE,
	SHEET
}
