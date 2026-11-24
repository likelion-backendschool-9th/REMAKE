package com.remake.ingredient.domain;

/** 재료를 쓴 경로. 냉장고에서 직접 기록하면 CONSUME, 요리 완료 차감이면 COOK이다(ingredient_used의 use_via). */
public enum ConsumeVia {
	CONSUME,
	COOK
}
