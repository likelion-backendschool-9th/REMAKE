package com.remake.recipe.application.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.recipe.application.port.in.GetRecipeIngredientsUseCase;
import com.remake.recipe.application.port.in.MealPlanRecipeCommand;
import com.remake.recipe.application.port.in.RecipeIngredientLine;
import com.remake.recipe.application.port.in.SaveMealPlanRecipesUseCase;
import com.remake.recipe.application.port.out.LoadRecipePort;
import com.remake.recipe.application.port.out.SaveRecipePort;
import com.remake.recipe.domain.Recipe;
import com.remake.recipe.domain.RecipeIngredient;
import com.remake.recipe.domain.RecipeSource;

import lombok.RequiredArgsConstructor;

/** 식단표(05)가 쓰는 레시피 기능: 칸 레시피 저장, 이미 있는 칸들의 재료 읽기. */
@Service
@RequiredArgsConstructor
public class MealPlanRecipeService implements SaveMealPlanRecipesUseCase, GetRecipeIngredientsUseCase {

	private final SaveRecipePort saveRecipePort;
	private final LoadRecipePort loadRecipePort;

	@Override
	@Transactional
	public List<Long> saveMealPlanRecipes(Long userId, List<MealPlanRecipeCommand> recipes) {
		return recipes.stream()
				.map(command -> saveRecipePort.save(toRecipe(userId, command)).id())
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Long, List<RecipeIngredientLine>> getIngredients(Long userId, Collection<Long> recipeIds) {
		Map<Long, List<RecipeIngredientLine>> result = new LinkedHashMap<>();
		loadRecipePort.loadIngredientsByRecipeIds(recipeIds, userId).forEach((recipeId, ingredients) ->
				result.put(recipeId, ingredients.stream().map(MealPlanRecipeService::toLine).toList()));
		return result;
	}

	private static Recipe toRecipe(Long userId, MealPlanRecipeCommand command) {
		List<RecipeIngredient> ingredients = command.ingredients().stream()
				.map(ingredient -> new RecipeIngredient(ingredient.ingredientId(), ingredient.name(),
						ingredient.amount(), ingredient.unit(), ingredient.categoryCode()))
				.toList();
		return Recipe.create(userId, RecipeSource.MEAL_PLAN, command.title(), command.servings(),
				command.cookMinutes(), command.tips(), null, ingredients, command.steps());
	}

	private static RecipeIngredientLine toLine(RecipeIngredient ingredient) {
		return new RecipeIngredientLine(ingredient.ingredientId(), ingredient.name(), ingredient.amount(),
				ingredient.unit());
	}
}
