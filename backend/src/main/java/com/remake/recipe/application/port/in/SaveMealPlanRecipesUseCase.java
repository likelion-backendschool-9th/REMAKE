package com.remake.recipe.application.port.in;

import java.util.List;

/** 식단표 칸 레시피 저장(source=MEAL_PLAN). 식단표 생성 작업이 칸을 저장하기 전에 부른다. */
public interface SaveMealPlanRecipesUseCase {

	/**
	 * 레시피들을 부른 쪽 트랜잭션 안에서 저장하고, 넘긴 순서대로 id를 돌려준다.
	 * 칸 저장이 실패해 롤백되면 레시피도 남지 않는다. 규칙을 어긴 레시피가 있으면 IllegalArgumentException.
	 */
	List<Long> saveMealPlanRecipes(Long userId, List<MealPlanRecipeCommand> recipes);
}
