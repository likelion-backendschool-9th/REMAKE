package com.remake.recipe.application.port.in;

import java.util.List;

import com.remake.recipe.domain.CookTime;
import com.remake.recipe.domain.CookingMethod;
import com.remake.recipe.domain.CookingTool;
import com.remake.recipe.domain.Cuisine;
import com.remake.recipe.domain.RecipeSource;
import com.remake.recipe.domain.Taste;
import com.remake.user.domain.AllergenCode;

/**
 * 레시피 생성 요청(API 28). 고른 옵션(id·enum)만 받는다. 자유 입력은 없다.
 *
 * @param mode          QUICK 또는 CUSTOM
 * @param servings      커스텀 식사 인원(필수). 빠른 생성은 무시하고 가구원 수를 쓴다
 * @param ingredientIds 커스텀에서 고른 내 냉장고 재료. 비어 있으면 냉장고 전체
 * @param allergenCodes 커스텀에서 고른 알레르기 칩 코드
 */
public record GenerateRecipeCommand(
		RecipeSource mode,
		Integer servings,
		List<Long> ingredientIds,
		Taste taste,
		CookTime cookTime,
		CookingTool tool,
		CookingMethod method,
		Cuisine cuisine,
		List<AllergenCode> allergenCodes) {

	public GenerateRecipeCommand {
		ingredientIds = ingredientIds == null ? List.of() : List.copyOf(ingredientIds);
		allergenCodes = allergenCodes == null ? List.of() : List.copyOf(allergenCodes);
	}

	public static GenerateRecipeCommand quick() {
		return new GenerateRecipeCommand(RecipeSource.QUICK, null, null, null, null, null, null, null, null);
	}
}
