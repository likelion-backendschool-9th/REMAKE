package com.remake.share.application.service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.cart.application.port.in.AddCartItemsUseCase;
import com.remake.cart.application.port.in.CartAdditionCommand;
import com.remake.cart.domain.CartSource;
import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.GetFridgeUseCase;
import com.remake.recipe.application.port.in.GetRecipeForSharingUseCase;
import com.remake.recipe.application.port.in.GetRecipeSaveStateUseCase;
import com.remake.recipe.application.port.in.SaveSharedRecipeReferenceUseCase;
import com.remake.share.application.port.in.SaveSharedRecipeUseCase;
import com.remake.share.application.port.out.LoadSharedRecipePort;
import com.remake.share.domain.SharedRecipe;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
class SharedRecipeSaveService implements SaveSharedRecipeUseCase {
	private final LoadSharedRecipePort posts;
	private final GetRecipeForSharingUseCase recipes;
	private final SaveSharedRecipeReferenceUseCase saves;
	private final GetRecipeSaveStateUseCase states;
	private final GetFridgeUseCase fridge;
	private final AddCartItemsUseCase cart;
	private final EventTracker events;

	@Override
	public SaveResult save(Long userId, Long sharedRecipeId) {
		SharedRecipe post = visiblePost(sharedRecipeId);
		int added = 0;
		if (saves.saveReference(userId, post.recipeId(), post.id())) {
			var recipe = recipes.getSharedContent(post.recipeId());
			Set<String> owned = fridge.getFridgeIngredients(userId, null, null, null).stream()
					.filter(i -> i.dDay() >= 0).map(i -> i.ingredient().name()).collect(Collectors.toSet());
			// 타인의 ingredientId·필요량 대신 공유 상세와 같은 이름 비교로 부족 재료를 판정한다.
			var missing = recipe.ingredients().stream().filter(i -> !owned.contains(i.name()))
					.map(i -> new CartAdditionCommand(recipe.id(), null, i.name(), i.amount(), i.unit(), i.categoryCode()))
					.toList();
			// 서버 확정 카테고리가 없는 사전 미등록 재료는 공통 UseCase가 INVALID_INPUT으로 거절한다.
			added = cart.add(userId, CartSource.SHARED_RECIPE, missing);
			events.track(EventName.RECIPE_SAVED, Screen.SHARED_DETAIL,
					Map.of("recipe_id", recipe.id(), "save_source", "shared", "cart_added_count", added),
					EventTrackCommand.ofUser(userId));
		}
		var state = states.getSaveState(userId, post.recipeId(), post.id());
		return new SaveResult(state.saveCount(), state.saved(), added);
	}

	@Override
	public UnsaveResult unsave(Long userId, Long sharedRecipeId) {
		SharedRecipe post = visiblePost(sharedRecipeId);
		saves.unsaveReference(userId, post.recipeId());
		var state = states.getSaveState(userId, post.recipeId(), post.id());
		return new UnsaveResult(state.saveCount(), state.saved());
	}

	private SharedRecipe visiblePost(Long id) {
		return posts.findById(id).filter(p -> p.status() == SharedRecipe.Status.VISIBLE)
				.orElseThrow(() -> new BusinessException(ErrorCode.SHARED_RECIPE_NOT_FOUND));
	}
}
