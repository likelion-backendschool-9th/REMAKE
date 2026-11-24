package com.remake.cart.application.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.cart.application.port.in.MoveCartItemsToFridgeUseCase;
import com.remake.cart.application.port.out.LoadCartItemPort;
import com.remake.cart.application.port.out.SaveCartItemPort;
import com.remake.cart.domain.CartFridgeQuantity;
import com.remake.cart.domain.CartItem;
import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.GetIngredientCategoriesUseCase;
import com.remake.ingredient.application.port.in.RegisterCartIngredientsUseCase;
import com.remake.ingredient.application.port.in.RegisterIngredientCommand;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class CartMoveService implements MoveCartItemsToFridgeUseCase {
	private final LoadCartItemPort cart;
	private final SaveCartItemPort changes;
	private final GetIngredientCategoriesUseCase categories;
	private final RegisterCartIngredientsUseCase ingredients;
	private final EventTracker events;

	@Override
	@Transactional
	public int move(Long userId, List<Long> ids) {
		List<Long> sorted = CartSelection.validate(userId, ids);
		List<CartItem> items = cart.loadOwnedForUpdate(userId, sorted);
		if (items.size() != sorted.size()) {
			throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
		}
		boolean wasSelectAll = items.size() == cart.countOwned(userId);
		List<RegisterIngredientCommand> commands = items.stream().map(item -> toCommand(userId, item)).toList();
		int added = ingredients.registerCartIngredients(commands).size();
		items.forEach(item -> changes.delete(item.id()));
		events.track(EventName.CART_MOVED_TO_FRIDGE, Screen.CART_LIST,
				Map.of("moved_count", added, "was_select_all", wasSelectAll), EventTrackCommand.ofUser(userId));
		return added;
	}

	private RegisterIngredientCommand toCommand(Long userId, CartItem item) {
		CartFridgeQuantity quantity;
		try {
			quantity = CartFridgeQuantity.from(item.amount(), item.unit());
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, e.getMessage());
		}
		var category = categories.getIngredientCategory(item.categoryId());
		return new RegisterIngredientCommand(userId, item.name(), category.code(), category.defaultStorage(),
				null, null, quantity.amount(), quantity.unit(), null, null);
	}
}
