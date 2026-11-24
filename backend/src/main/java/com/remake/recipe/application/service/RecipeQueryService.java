package com.remake.recipe.application.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.recipe.application.port.in.GetRecipeListItemsUseCase;
import com.remake.recipe.application.port.in.GetRecipeUseCase;
import com.remake.recipe.application.port.in.RecipeListItem;
import com.remake.recipe.application.port.out.LoadRecipePort;
import com.remake.recipe.application.port.out.RecipeHeader;
import com.remake.recipe.application.port.out.SavedRecipePort;
import com.remake.recipe.domain.IngredientSummary;
import com.remake.recipe.domain.Recipe;
import com.remake.recipe.domain.SavedRecipe;

import lombok.RequiredArgsConstructor;

/** 레시피 상세(API 30)와 다른 영역용 목록 한 줄 읽기. 볼 수 있는 레시피 = 내가 만들었거나 내 저장 목록에 있는 것. */
@Service
@RequiredArgsConstructor
class RecipeQueryService implements GetRecipeUseCase, GetRecipeListItemsUseCase {

	private final LoadRecipePort loadRecipePort;
	private final SavedRecipePort savedRecipePort;

	@Override
	@Transactional(readOnly = true)
	public RecipeDetail getRecipe(Long userId, Long recipeId) {
		SavedRecipe saved = savedRecipePort.find(userId, recipeId).orElse(null);
		Recipe recipe = (saved != null ? loadRecipePort.loadById(recipeId)
				: loadRecipePort.loadByIdAndUserId(recipeId, userId))
				.orElseThrow(() -> new BusinessException(ErrorCode.RECIPE_NOT_FOUND));
		return new RecipeDetail(recipe, saved != null, saved != null && saved.favorite());
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Long, RecipeListItem> getListItems(Long userId, Collection<Long> recipeIds) {
		if (recipeIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, Boolean> favorites = savedRecipePort.findFavorites(userId, recipeIds);
		Map<Long, RecipeListItem> items = new LinkedHashMap<>();
		loadRecipePort.loadHeaders(recipeIds).values().stream()
				.filter(h -> userId.equals(h.userId()) || favorites.containsKey(h.recipeId()))
				.forEach(h -> items.put(h.recipeId(), toListItem(h, favorites.getOrDefault(h.recipeId(), false))));
		return items;
	}

	static RecipeListItem toListItem(RecipeHeader header, boolean favorite) {
		IngredientSummary summary = IngredientSummary.of(header.ingredientNames());
		return new RecipeListItem(header.recipeId(), header.title(), summary.summary(), summary.extraCount(),
				favorite);
	}
}
