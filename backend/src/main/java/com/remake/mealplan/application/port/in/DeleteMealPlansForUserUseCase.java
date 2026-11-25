package com.remake.mealplan.application.port.in;

/** 회원 탈퇴 때 부른다. 그 회원의 식단표(칸 CASCADE)를 모두 지운다. */
public interface DeleteMealPlansForUserUseCase {

	void deleteMealPlansForUser(Long userId);
}
