package com.remake.receipt.application.port.in;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;

import io.swagger.v3.oas.annotations.media.Schema;

public interface ExpenseHistoryUseCase {

	MonthlyPattern patterns(Long userId, YearMonth month);

	DailyItems dailyItems(Long userId, LocalDate date);

	DiscardedItems discardedItems(Long userId, YearMonth month);

	record MonthlyPattern(YearMonth month, BigInteger totalAmount,
			@Schema(nullable = true) BigInteger averageDiffAmount,
			BigInteger discardedAmount, List<Category> categories, List<DailyTotal> dailyTotals) {
	}

	@Schema(name = "ExpenseCategory")
	record Category(IngredientCategoryCode categoryCode, BigInteger amount, BigDecimal ratio) {
	}

	record DailyTotal(LocalDate date, BigInteger amount) {
	}

	record DailyItems(LocalDate date, BigInteger totalAmount, List<Item> items) {
	}

	@Schema(name = "ExpenseDailyItem")
	record Item(String name, IngredientCategoryCode categoryCode, String imageUrl, StorageLocation storage,
			BigDecimal amount, IngredientUnit unit, long price) {
	}

	record DiscardedItems(YearMonth month, BigInteger totalAmount, List<DiscardedItem> items) {
	}

	record DiscardedItem(String name, IngredientCategoryCode categoryCode, LocalDate discardedOn,
			@Schema(nullable = true) Long amount) {
	}
}
