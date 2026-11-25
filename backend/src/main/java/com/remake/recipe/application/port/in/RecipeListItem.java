package com.remake.recipe.application.port.in;

/**
 * 목록 한 줄(저장 목록 API 33, 식단표 하루 조회·칸 교체 API 40·42).
 *
 * @param ingredientSummary    앞 3개 재료를 " · "로 이은 것(IngredientSummary)
 * @param extraIngredientCount 보여 주지 않은 재료 개수. 화면이 "+N"으로 표시
 * @param favorite             저장했고 즐겨찾기를 켰으면 true
 */
public record RecipeListItem(Long recipeId, String title, String ingredientSummary, int extraIngredientCount,
		boolean favorite) {
}
