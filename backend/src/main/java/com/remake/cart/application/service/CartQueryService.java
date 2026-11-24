package com.remake.cart.application.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.cart.application.port.in.GetCartItemsUseCase;
import com.remake.cart.application.port.out.LoadCartItemPort;
import com.remake.ingredient.application.port.in.GetIngredientCategoriesUseCase;
import com.remake.ingredient.domain.IngredientCategory;
import com.remake.recipe.application.port.in.GetCartRecipeTitlesUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class CartQueryService implements GetCartItemsUseCase {
	private final LoadCartItemPort cart;
	private final GetIngredientCategoriesUseCase categories;
	private final GetCartRecipeTitlesUseCase recipes;

	@Override
	@Transactional(readOnly = true)
	public List<Item> getItems(Long userId) {
		var items = cart.loadOwned(userId);
		if (items.isEmpty()) {
			return List.of();
		}
		Map<Long, IngredientCategory> byId = categories.getIngredientCategories().stream()
				.collect(Collectors.toMap(IngredientCategory::id, Function.identity()));
		var titles = recipes.getTitles(items.stream().map(i -> i.recipeId())
				.filter(Objects::nonNull).distinct().toList());
		return items.stream().map(i -> {
			IngredientCategory category = byId.get(i.categoryId());
			return new Item(i.id(), i.recipeId(), i.recipeId() == null ? null : titles.get(i.recipeId()), i.name(), category.code(),
					category.imageUrl(), i.amount(), i.unit());
		}).toList();
	}
}
