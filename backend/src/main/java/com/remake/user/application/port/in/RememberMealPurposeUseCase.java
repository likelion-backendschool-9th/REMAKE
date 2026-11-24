package com.remake.user.application.port.in;

/**
 * 식단표 생성 설정에서 마지막으로 고른 식단 목적을 기억한다(users.last_meal_purpose, 다음 생성의 기본값).
 * 식단표(05)가 생성 접수(API 38) 때 부른다. 프로필(API 5)의 lastMealPurpose로 읽는다.
 */
public interface RememberMealPurposeUseCase {

	/** @param mealPurpose 식단 목적 코드(ERD 부록 C, 예: BALANCED·HIGH_PROTEIN). 사용자가 쓴 글이 아니다 */
	void rememberMealPurpose(Long userId, String mealPurpose);
}
