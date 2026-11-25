package com.remake.recipe.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.GetFridgeUseCase;
import com.remake.recipe.application.port.in.GetSavedRecipesUseCase;
import com.remake.recipe.application.port.in.RecipeListItem;
import com.remake.recipe.application.port.in.SaveRecipeUseCase;
import com.remake.recipe.application.port.out.LoadRecipePort;
import com.remake.recipe.application.port.out.RecipeHeader;
import com.remake.recipe.application.port.out.SavedRecipePort;
import com.remake.recipe.domain.SavedRecipe;
import com.remake.recipe.domain.SavedRecipeFilter;
import com.remake.recipe.domain.SavedRecipeSort;

import lombok.RequiredArgsConstructor;

/** 레시피 저장·저장 취소·즐겨찾기(API 34~37)와 저장 목록(API 33). */
@Service
@RequiredArgsConstructor
class RecipeSaveService implements SaveRecipeUseCase, GetSavedRecipesUseCase {

	private final LoadRecipePort loadRecipePort;
	private final SavedRecipePort savedRecipePort;
	private final GetFridgeUseCase getFridgeUseCase;
	private final EventTracker eventTracker;
	private final Clock clock;

	/**
	 * 내가 만든 레시피를 저장한다. 처음 저장할 때만 E44 recipe_saved를 남긴다.
	 * 재료 보충으로 들어간 부족 재료의 장바구니 담기(#57: 장바구니에 없는 재료만 추가)는 장바구니 영역의 AddCartItemsUseCase가
	 * 생기면 여기서 부른다. 그 전까지 cartAddedCount는 0이다.
	 */
	@Override
	@Transactional
	public SaveResult save(Long userId, Long recipeId) {
		if (!loadRecipePort.existsByIdAndUserId(recipeId, userId)) {
			throw new BusinessException(ErrorCode.RECIPE_NOT_FOUND);
		}
		boolean inserted = savedRecipePort.insertIfAbsent(userId, recipeId, LocalDateTime.now(clock));
		int cartAddedCount = 0;
		if (inserted) {
			// 이벤트 명세: 내 레시피는 own, 공유 저장은 shared.
			eventTracker.track(EventName.RECIPE_SAVED, Screen.RECIPE_DETAIL, Map.of(
					"recipe_id", recipeId,
					"save_source", "own",
					"cart_added_count", cartAddedCount), EventTrackCommand.ofUser(userId));
		}
		return new SaveResult(true, cartAddedCount);
	}

	@Override
	@Transactional
	public void unsave(Long userId, Long recipeId) {
		requireSaved(userId, recipeId);
		savedRecipePort.delete(userId, recipeId);
	}

	@Override
	@Transactional
	public boolean setFavorite(Long userId, Long recipeId, boolean favorite) {
		SavedRecipe saved = requireSaved(userId, recipeId);
		if (saved.favorite() != favorite) {
			savedRecipePort.updateFavorite(userId, recipeId, favorite);
		}
		return favorite;
	}

	@Override
	@Transactional(readOnly = true)
	public SavedRecipes getSavedRecipes(Long userId, SavedRecipeFilter filter, SavedRecipeSort sort, int page,
			int size) {
		Set<String> availableNames = filter == SavedRecipeFilter.AVAILABLE ? usableFridgeNames(userId) : null;
		Page<SavedRecipe> saved = savedRecipePort.findPage(userId, filter == SavedRecipeFilter.FAVORITE,
				availableNames, sort, page, size);
		Map<Long, RecipeHeader> headers = loadRecipePort
				.loadHeaders(saved.getContent().stream().map(SavedRecipe::recipeId).toList());
		Page<RecipeListItem> items = saved
				.map(s -> RecipeQueryService.toListItem(headers.get(s.recipeId()), s.favorite()));
		return new SavedRecipes(savedRecipePort.countByUserId(userId), items);
	}

	/** 볼 수 있는 레시피인데 저장하지 않았으면 SAVED_RECIPE_NOT_FOUND, 볼 수 없으면 RECIPE_NOT_FOUND. */
	private SavedRecipe requireSaved(Long userId, Long recipeId) {
		return savedRecipePort.find(userId, recipeId).orElseThrow(() -> new BusinessException(
				loadRecipePort.existsByIdAndUserId(recipeId, userId) ? ErrorCode.SAVED_RECIPE_NOT_FOUND
						: ErrorCode.RECIPE_NOT_FOUND));
	}

	/**
	 * "바로 가능" 비교에 쓰는 이름: 내 냉장고 재료(소비기한 지난 것은 뺌, 레시피 생성과 같은 기준) + 물.
	 * 물은 냉장고에 등록하는 품목이 아니라 없어도 만들 수 있다(#158, GeneratedRecipeValidator.WATER).
	 */
	private Set<String> usableFridgeNames(Long userId) {
		Set<String> names = getFridgeUseCase.getFridgeIngredients(userId, null, null, null).stream()
				.filter(f -> f.dDay() >= 0)
				.map(f -> f.ingredient().name())
				.collect(Collectors.toCollection(HashSet::new));
		names.add(GeneratedRecipeValidator.WATER);
		return names;
	}
}
