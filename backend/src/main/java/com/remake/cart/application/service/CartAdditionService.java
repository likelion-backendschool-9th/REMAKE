package com.remake.cart.application.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.cart.application.port.in.AddCartItemsUseCase;
import com.remake.cart.application.port.in.CartAdditionCommand;
import com.remake.cart.application.port.out.InsertCartItemPort;
import com.remake.cart.domain.CartItem;
import com.remake.cart.domain.CartSource;
import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.GetIngredientCatalogUseCase;
import com.remake.ingredient.application.port.in.GetIngredientCategoriesUseCase;
import com.remake.recipe.domain.RecipeUnit;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class CartAdditionService implements AddCartItemsUseCase {
	private final InsertCartItemPort insert;
	private final GetIngredientCatalogUseCase catalog;
	private final GetIngredientCategoriesUseCase categories;
	private final EventTracker events;
	private final Clock clock;

	private record Key(Long recipeId, String name, RecipeUnit unit) { }

	@Override
	@Transactional
	public int add(Long userId, CartSource source, List<CartAdditionCommand> commands) {
		if (userId == null || userId <= 0 || source == null || commands == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		Map<Key, CartItem> prepared = new LinkedHashMap<>();
		for (CartAdditionCommand command : commands) {
			validate(command, source);
			String name = command.ingredientName().strip();
			if (command.amount() == null || command.unit() == RecipeUnit.PINCH
					|| command.unit() == RecipeUnit.TO_TASTE || name.equals("물")) {
				continue;
			}
			Key key = new Key(command.recipeId(), name, command.unit());
			CartItem previous = prepared.get(key);
			if (previous != null) {
				if (source == CartSource.MEAL_PLAN) {
					BigDecimal total = previous.amount().add(command.amount());
					validateAmount(total);
					prepared.put(key, new CartItem(null, userId, previous.recipeId(), previous.mealPlanSlotId(),
							previous.categoryId(), name, total, previous.unit()));
				}
				continue;
			}
			var entry = catalog.findCatalogItemByName(name);
			Long categoryId;
			if (entry.isPresent()) {
				categoryId = entry.get().categoryId();
			} else if (command.categoryCode() != null) {
				categoryId = categories.getIngredientCategoryByCode(command.categoryCode()).id();
			} else {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "사전에 없는 재료는 서버 확정 카테고리가 필요합니다.");
			}
			prepared.put(key, new CartItem(null, userId, command.recipeId(), command.mealPlanSlotId(),
					categoryId, name, command.amount(), command.unit()));
		}
		int added = 0;
		LocalDateTime now = LocalDateTime.now(clock);
		for (CartItem item : prepared.values().stream().sorted(Comparator.comparing(CartItem::recipeId)
				.thenComparing(CartItem::name).thenComparing(item -> item.unit().name())).toList()) {
			if (insert.insertIfAbsent(item, now)) {
				added++;
			}
		}
		if (added > 0) {
			Screen screen = switch (source) {
				case OWN_RECIPE -> Screen.RECIPE_DETAIL;
				case SHARED_RECIPE -> Screen.SHARED_DETAIL;
				case MEAL_PLAN -> Screen.MEAL_PLAN_SETUP;
			};
			events.track(EventName.CART_ITEM_ADDED, screen,
					Map.of("cart_source", source.eventValue(), "item_count", added), EventTrackCommand.ofUser(userId));
		}
		return added;
	}

	private void validate(CartAdditionCommand command, CartSource source) {
		if (command == null || command.recipeId() == null || command.recipeId() <= 0
				|| command.ingredientName() == null || command.ingredientName().isBlank()
				|| command.ingredientName().strip().length() > 50 || command.unit() == null
				|| (command.mealPlanSlotId() != null && (command.mealPlanSlotId() <= 0 || source != CartSource.MEAL_PLAN))) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		if (command.amount() != null) {
			validateAmount(command.amount());
		}
	}

	private void validateAmount(BigDecimal value) {
		try {
			CartItem.validateAmount(value);
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, e.getMessage());
		}
	}
}
