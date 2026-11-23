package com.remake.ingredient.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.ResolveIngredientUnitUseCase;
import com.remake.ingredient.application.port.out.LoadIngredientCatalogPort;
import com.remake.ingredient.application.port.out.LoadIngredientCategoryPort;
import com.remake.ingredient.domain.IngredientCatalogItem;
import com.remake.ingredient.domain.IngredientCategory;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.IngredientUnitPolicy;

import lombok.RequiredArgsConstructor;

/** 재료명으로 단위를 정한다. 사전을 읽는 일만 하고, 후보 중에 고르는 규칙은 IngredientUnitPolicy에 있다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientUnitService implements ResolveIngredientUnitUseCase {

	private final LoadIngredientCatalogPort loadIngredientCatalogPort;
	private final LoadIngredientCategoryPort loadIngredientCategoryPort;

	@Override
	public IngredientUnit resolveUnit(String name, IngredientCategoryCode categoryCode) {
		if (name == null || name.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "재료명은 필수입니다.");
		}
		String stripped = name.strip();

		var exact = loadIngredientCatalogPort.loadByName(stripped);
		if (exact.isPresent()) {
			return exact.get().unit();
		}
		if (categoryCode == null) {
			return IngredientUnitPolicy.DEFAULT_UNIT;
		}

		IngredientCategory category = loadIngredientCategoryPort.loadByCode(categoryCode)
				.orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_CATEGORY_NOT_FOUND));
		return IngredientUnitPolicy
				.findContainedItem(stripped, loadIngredientCatalogPort.loadByCategoryId(category.id()))
				.map(IngredientCatalogItem::unit)
				.orElse(IngredientUnitPolicy.DEFAULT_UNIT);
	}
}
