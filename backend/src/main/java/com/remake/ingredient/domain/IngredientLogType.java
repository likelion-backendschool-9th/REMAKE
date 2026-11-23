package com.remake.ingredient.domain;

/** 재료 기록 종류. 사용했어요(USED)와 버렸어요(DISCARDED). DB와 JSON에는 이름 그대로 들어간다. */
public enum IngredientLogType {
	USED,
	DISCARDED
}
