package com.remake.ingredient.domain;

/** 냉장고 화면의 소비기한 상태. 판정은 ExpiryPolicy.statusOf가 한다. */
public enum ExpiryStatus {
	DANGER,
	SOON,
	OK
}
