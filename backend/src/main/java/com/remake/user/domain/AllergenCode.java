package com.remake.user.domain;

/**
 * 제외 재료(알레르기) 칩 19개. ERD 부록 B·피그마 p1.2 순서.
 * API의 allergenCodes와 DB allergen_code에 같은 이름을 쓴다.
 */
public enum AllergenCode {
	EGG,
	MILK,
	BUCKWHEAT,
	PEANUT,
	SOYBEAN,
	WHEAT,
	MACKEREL,
	CRUSTACEAN,
	PORK,
	SHRIMP,
	SULFITE,
	PEACH,
	SQUID,
	CHICKEN,
	BEEF,
	OYSTER,
	CRAB,
	PINE_NUT,
	SHELLFISH;

	/** 모르는 코드면 null. */
	public static AllergenCode fromWire(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return AllergenCode.valueOf(value.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}
}
