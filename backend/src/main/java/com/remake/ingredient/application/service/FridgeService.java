package com.remake.ingredient.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.GetFridgeUseCase;
import com.remake.ingredient.application.port.out.LoadIngredientCategoryPort;
import com.remake.ingredient.application.port.out.LoadIngredientPort;
import com.remake.ingredient.domain.ExpiryPolicy;
import com.remake.ingredient.domain.ExpiryStatus;
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

	private final LoadIngredientPort loadIngredientPort;
	private final LoadIngredientCategoryPort loadIngredientCategoryPort;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public List<FridgeIngredient> getFridgeIngredients(Long userId, ExpiryStatus status, StorageLocation storage,
			IngredientCategoryCode categoryCode) {
		List<IngredientCategory> categories = loadIngredientCategoryPort.loadAll();
		Long categoryId = null;
		if (categoryCode != null) {
			categoryId = categories.stream().filter(c -> c.code() == categoryCode).map(IngredientCategory::id)
					.findFirst()
					.orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_CATEGORY_NOT_FOUND));
		}
		Map<Long, IngredientCategory> categoriesById = categories.stream()
				.collect(Collectors.toMap(IngredientCategory::id, Function.identity(), (a, b) -> a));

		LocalDate today = LocalDate.now(clock);
		return loadIngredientPort.loadActiveByUserId(userId, storage, categoryId).stream()
				.map(ingredient -> toFridgeIngredient(ingredient, categoriesById, today))
				// 상태는 오늘 날짜로 계산하는 값이라 DB가 아니라 여기서 거른다. 목록은 페이징이 없어 한 번에 거르면 된다.
				.filter(item -> status == null || item.status() == status)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public FridgeSummary getFridgeSummary(Long userId) {
		return FridgeSummary.of(loadIngredientPort.loadActiveExpirationDates(userId), LocalDate.now(clock));
	}

	private FridgeIngredient toFridgeIngredient(Ingredient ingredient, Map<Long, IngredientCategory> categoriesById,
			LocalDate today) {
		long dDay = ExpiryPolicy.dDay(ingredient.expirationDate(), today);
		return new FridgeIngredient(ingredient, categoriesById.get(ingredient.categoryId()), dDay,
				ExpiryPolicy.statusOf(dDay));
	}
}
