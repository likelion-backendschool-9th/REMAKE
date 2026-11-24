package com.remake.recipe.adapter.in.web;

import java.util.List;

import com.remake.recipe.application.port.in.GenerateRecipeCommand;
import com.remake.recipe.domain.CookTime;
import com.remake.recipe.domain.CookingMethod;
import com.remake.recipe.domain.CookingTool;
import com.remake.recipe.domain.Cuisine;
import com.remake.recipe.domain.RecipeSource;
import com.remake.recipe.domain.Taste;
import com.remake.user.domain.AllergenCode;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * API 28 요청. 빠른 생성 { mode: "QUICK" } / 커스텀 { mode: "CUSTOM", servings, ingredientIds, taste?, cookTime?, tool?,
 * method?, cuisine?, allergenCodes }. 고른 값(id·enum)만 받는다.
 */
public record GenerateRecipeRequest(
		@Schema(description = "QUICK 또는 CUSTOM", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull RecipeSource mode,
		@Schema(description = "커스텀 식사 인원(1~50). 빠른 생성은 가구원 수를 쓴다") Integer servings,
		@Schema(description = "커스텀에서 고른 내 냉장고 재료 id. 비우면 냉장고 전체") @Size(max = 100) List<@NotNull Long> ingredientIds,
		Taste taste,
		CookTime cookTime,
		CookingTool tool,
		CookingMethod method,
		Cuisine cuisine,
		@Schema(description = "커스텀에서 고른 알레르기 칩 코드(ERD 부록 B 19개). 모르는 코드는 400 INVALID_INPUT") @Size(max = 19) List<@NotNull AllergenCode> allergenCodes) {

	GenerateRecipeCommand toCommand() {
		return new GenerateRecipeCommand(mode, servings, ingredientIds, taste, cookTime, tool, method, cuisine,
				allergenCodes);
	}
}
