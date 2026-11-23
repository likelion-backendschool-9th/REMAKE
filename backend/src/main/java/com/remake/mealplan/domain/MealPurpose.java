package com.remake.mealplan.domain;

/** 식단 목적. 기본값은 BALANCED. 의료 목적·칼로리 목표는 없다. */
public enum MealPurpose {
	BALANCED,
	DIET,
	HIGH_PROTEIN,
	SIMPLE,
	SAVING
}
