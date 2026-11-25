package com.remake.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.recipe.application.port.in.GetRecipeIngredientsUseCase;
import com.remake.recipe.application.port.in.MealPlanRecipeCommand;
import com.remake.recipe.application.port.in.RecipeIngredientLine;
import com.remake.recipe.application.port.in.SaveMealPlanRecipesUseCase;
import com.remake.recipe.domain.RecipeUnit;
import com.remake.support.IntegrationTestSupport;

// 식단표가 쓰는 레시피 기능: 칸 레시피 여러 개 저장, 여러 레시피의 재료 줄 읽기.
class MealPlanRecipeServiceTest extends IntegrationTestSupport {

	@Autowired
	private SaveMealPlanRecipesUseCase saveMealPlanRecipesUseCase;

	@Autowired
	private GetRecipeIngredientsUseCase getRecipeIngredientsUseCase;

	private static MealPlanRecipeCommand recipe(String title) {
		return new MealPlanRecipeCommand(title, 2, 15, "  ", List.of(
				new MealPlanRecipeCommand.Ingredient(null, "두부", BigDecimal.valueOf(300), RecipeUnit.G),
				new MealPlanRecipeCommand.Ingredient(null, "소금", null, RecipeUnit.TO_TASTE)),
				List.of("두부를 썬다", "굽는다"));
	}

	@Test
	void 넘긴_순서대로_식단표_레시피로_저장하고_id를_돌려준다() {
		Long userId = createUser();

		List<Long> ids = saveMealPlanRecipesUseCase.saveMealPlanRecipes(userId,
				List.of(recipe("두부 구이"), recipe("두부 조림")));

		assertThat(ids).hasSize(2);
		assertThat(jdbcTemplate.queryForList("SELECT title FROM recipes WHERE id IN (?, ?) ORDER BY id", String.class,
				ids.get(0), ids.get(1))).containsExactly("두부 구이", "두부 조림");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM recipes WHERE user_id = ? AND source = 'MEAL_PLAN' AND tips IS NULL",
				Integer.class, userId)).isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM recipe_steps WHERE recipe_id = ?", Integer.class,
				ids.get(0))).isEqualTo(2);
	}

	@Test
	void 재료_줄은_내_레시피만_레시피별_화면_순서로_돌려준다() {
		Long userId = createUser();
		Long other = createUser();
		Long mine = saveMealPlanRecipesUseCase.saveMealPlanRecipes(userId, List.of(recipe("두부 구이"))).getFirst();
		Long others = createRecipe(other);
		jdbcTemplate.update("""
				INSERT INTO recipe_ingredients (recipe_id, name, amount, unit, sort_order, created_at)
				VALUES (?, '김치', 100, 'G', 0, ?)
				""", others, LocalDateTime.now(clock));

		Map<Long, List<RecipeIngredientLine>> lines = getRecipeIngredientsUseCase.getIngredients(userId,
				List.of(mine, others));

		assertThat(lines).containsOnlyKeys(mine);
		assertThat(lines.get(mine)).extracting(RecipeIngredientLine::name).containsExactly("두부", "소금");
		assertThat(getRecipeIngredientsUseCase.getIngredients(userId, List.of())).isEmpty();
	}
}
