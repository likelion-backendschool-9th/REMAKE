package com.remake.share.application.service;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.GetFridgeUseCase;
import com.remake.recipe.application.port.in.GetRecipeForSharingUseCase;
import com.remake.recipe.application.port.in.GetRecipeSaveStateUseCase;
import com.remake.recipe.domain.Recipe;
import com.remake.share.application.port.in.GetSharedRecipeUseCase;
import com.remake.share.application.port.out.LoadSharedRecipePort;
import com.remake.share.domain.SharedRecipe;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class SharedRecipeQueryService implements GetSharedRecipeUseCase {
	// #158: 물만 냉장고에 등록하지 않아도 항상 보유한 것으로 본다.
	private static final String WATER = "물";
	private final LoadSharedRecipePort sharedRecipes;
	private final GetRecipeForSharingUseCase recipes;
	private final GetRecipeSaveStateUseCase saves;
	private final GetFridgeUseCase fridge;

	@Override
	@Transactional(readOnly = true)
	public Detail getDetail(Long userId, Long id) {
		SharedRecipe post = sharedRecipes.findById(id)
				.filter(p -> p.status() == SharedRecipe.Status.VISIBLE)
				.orElseThrow(() -> new BusinessException(ErrorCode.SHARED_RECIPE_NOT_FOUND));
		Recipe recipe = recipes.getSharedContent(post.recipeId());
		var state = saves.getSaveState(userId, recipe.id(), post.id());
		// ERD의 이름 비교 규칙. 다른 사람의 냉장고·원본 ingredientId·필요 수량은 보유 판정에 쓰지 않는다.
		Set<String> ownedNames = fridge.getFridgeIngredients(userId, null, null, null).stream()
				.filter(i -> i.dDay() >= 0)
				.map(i -> i.ingredient().name()).collect(Collectors.toSet());
		return new Detail(post.id(), post.title(), "/api/v1/user/shared-recipe-photos/" + post.id(),
				recipe.servings(), recipe.cookMinutes(), recipe.tips(),
				recipe.ingredients().stream().map(i -> new Ingredient(i.name(), i.amount(), i.unit(),
						WATER.equals(i.name()) || ownedNames.contains(i.name()))).toList(),
				IntStream.range(0, recipe.steps().size())
						.mapToObj(i -> new Step(i + 1, recipe.steps().get(i))).toList(),
				state.saveCount(), state.saved());
	}
}
