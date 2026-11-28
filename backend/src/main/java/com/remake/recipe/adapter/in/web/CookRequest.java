package com.remake.recipe.adapter.in.web;

import java.math.BigDecimal;
import java.util.List;

import com.remake.recipe.application.port.in.CookRecipeUseCase.CookItem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 요리 완료(API 32). { items: [{ ingredientId, amount }], mealPlanSlotId? }. items는 빈 목록이어도 된다(차감 없이 요리 완료).
 * amount가 0 이하인 것은 COOK_ITEM_INVALID라 여기서 막지 않는다(서비스가 낸다).
 */
public record CookRequest(@NotNull List<@NotNull @Valid Item> items, @Positive Long mealPlanSlotId) {

	@Schema(name = "CookRequestItem")
	public record Item(@NotNull @Positive Long ingredientId, @NotNull BigDecimal amount) {
	}

	List<CookItem> toItems() {
		return items.stream().map(i -> new CookItem(i.ingredientId(), i.amount())).toList();
	}
}
