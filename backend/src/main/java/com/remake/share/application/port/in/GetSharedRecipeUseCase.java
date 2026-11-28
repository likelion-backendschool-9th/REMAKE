package com.remake.share.application.port.in;

import java.math.BigDecimal;
import java.util.List;

import com.remake.recipe.domain.RecipeUnit;

import io.swagger.v3.oas.annotations.media.Schema;

public interface GetSharedRecipeUseCase {
	Detail getDetail(Long userId, Long id);
	/** 작성자·원본 ID와 원본 재료 ID는 응답 모델에 포함하지 않는다. */
	@Schema(name = "SharedRecipeDetail")
	record Detail(Long id, String title, @Schema(nullable = true) String imageUrl, int servings, int cookMinutes,
			@Schema(nullable = true) String tips,
			List<Ingredient> ingredients, List<Step> steps, long saveCount, boolean saved, boolean canDelete) { }
	@Schema(name = "SharedRecipeIngredient")
	record Ingredient(String name, @Schema(nullable = true) BigDecimal amount, RecipeUnit unit, boolean have) { }
	@Schema(name = "SharedRecipeStep")
	record Step(int stepNo, String content) { }
}
