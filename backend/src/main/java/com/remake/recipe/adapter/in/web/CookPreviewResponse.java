package com.remake.recipe.adapter.in.web;

import java.math.BigDecimal;
import java.util.List;

import com.remake.ingredient.domain.IngredientUnit;
import com.remake.recipe.application.port.in.CookRecipeUseCase.PreviewItem;

/**
 * 요리 후 차감 미리보기(API 31). { items: [{ ingredientId, name, imageUrl, amount, unit, remainingAmount }] }.
 * amount·unit은 냉장고 단위다. amount가 null이면 단위 종류가 달라 서버가 양을 정하지 못한 것이라 사용자가 직접 정한다.
 */
public record CookPreviewResponse(List<Item> items) {

	public record Item(Long ingredientId, String name, String imageUrl, BigDecimal amount, IngredientUnit unit,
			BigDecimal remainingAmount) {
	}

	static CookPreviewResponse from(List<PreviewItem> items) {
		return new CookPreviewResponse(items.stream()
				.map(i -> new Item(i.ingredientId(), i.name(), i.imageUrl(), i.amount(), i.unit(), i.remainingAmount()))
				.toList());
	}
}
