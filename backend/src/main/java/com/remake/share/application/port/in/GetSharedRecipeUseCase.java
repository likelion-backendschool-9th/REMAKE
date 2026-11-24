package com.remake.share.application.port.in;

import java.math.BigDecimal;
import java.util.List;

import com.remake.recipe.domain.RecipeUnit;

public interface GetSharedRecipeUseCase {
	Detail getDetail(Long userId, Long id);
	/** 작성자·원본 ID와 원본 재료 ID는 응답 모델에 포함하지 않는다. */
	record Detail(Long id, String title, String imageUrl, int servings, int cookMinutes, String tips,
			List<Ingredient> ingredients, List<Step> steps, long saveCount, boolean saved) { }
	record Ingredient(String name, BigDecimal amount, RecipeUnit unit, boolean have) { }
	record Step(int stepNo, String content) { }
}
