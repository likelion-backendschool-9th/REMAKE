package com.remake.ingredient.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.GetFridgeUseCase;
import com.remake.ingredient.application.port.out.LoadIngredientCategoryPort;
import com.remake.ingredient.application.port.out.LoadIngredientPort;
import com.remake.ingredient.domain.ExpiryPolicy;
import com.remake.ingredient.domain.FridgeIngredient;
import com.remake.ingredient.domain.FridgeSummary;
import com.remake.ingredient.domain.Ingredient;
import com.remake.ingredient.domain.IngredientCategory;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.StorageLocation;

import lombok.RequiredArgsConstructor;

/**
 * 냉장고 화면 조회. 오늘 날짜는 주입된 Clock으로만 구하고, 상태는 ExpiryPolicy 한 곳에서 판정해
 * 요약 수치와 목록 상태가 어긋나지 않게 한다. FINISHED 재료는 목록과 요약에서 제외한다.
 */
@Service
@RequiredArgsConstructor
public class FridgeService implements GetFridgeUseCase {

	private static final int MAX_KEYWORD_LENGTH = 50;

	private final LoadIngredientPort loadIngredientPort;
	private final LoadIngredientCategoryPort loadIngredientCategoryPort;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public List<FridgeIngredient> getFridgeIngredients(Long userId, StorageLocation storage,
			IngredientCategoryCode categoryCode, String keyword) {
		if (keyword != null && keyword.strip().length() > MAX_KEYWORD_LENGTH) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "검색어는 50자 이하여야 합니다.");
		}
		List<IngredientCategory> categories = loadIngredientCategoryPort.loadAll();
		Long categoryId = null;
		if (categoryCode != null) {
			categoryId = categories.stream().filter(c -> c.code() == categoryCode).map(IngredientCategory::id)
					.findFirst()
					.orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_CATEGORY_NOT_FOUND));
		}
		Map<Long, IngredientCategoryCode> codes = categories.stream()
				.collect(Collectors.toMap(IngredientCategory::id, IngredientCategory::code, (a, b) -> a));

		LocalDate today = LocalDate.now(clock);
		return loadIngredientPort.loadActiveByUserId(userId, storage, categoryId, keyword).stream()
				.map(ingredient -> toFridgeIngredient(ingredient, codes, today))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public FridgeSummary getFridgeSummary(Long userId) {
		return FridgeSummary.of(loadIngredientPort.loadActiveExpirationDates(userId), LocalDate.now(clock));
	}

	private FridgeIngredient toFridgeIngredient(Ingredient ingredient, Map<Long, IngredientCategoryCode> codes,
			LocalDate today) {
		long dDay = ExpiryPolicy.dDay(ingredient.expirationDate(), today);
		return new FridgeIngredient(ingredient, codes.get(ingredient.categoryId()), dDay, ExpiryPolicy.statusOf(dDay));
	}
}
