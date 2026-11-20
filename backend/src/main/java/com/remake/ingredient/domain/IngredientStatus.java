package com.remake.ingredient.domain;

/** 재료 상태. 남은 양이 0이 되면 FINISHED. DB와 JSON에는 이름 그대로 들어간다. */
public enum IngredientStatus {
	ACTIVE,
	FINISHED
}
