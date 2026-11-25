package com.remake.receipt.application.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.receipt.application.port.in.ExpenseHistoryUseCase;
import com.remake.receipt.application.port.out.LoadExpenseHistoryPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class ExpenseHistoryService implements ExpenseHistoryUseCase {

	private static final int QUERY_MONTHS = 6;
	private static final int RATIO_SCALE = 8;

	private final LoadExpenseHistoryPort expenses;
	private final Clock clock;

	@Override
	public MonthlyPattern patterns(Long userId, YearMonth month) {
		YearMonth current = YearMonth.now(clock);
		YearMonth selected = month == null ? current : month;
		YearMonth earliest = earliestMonth(current);
		validateMonth(selected, current);

		var categories = new EnumMap<IngredientCategoryCode, BigInteger>(IngredientCategoryCode.class);
		var dailyTotals = new TreeMap<LocalDate, BigInteger>();
		var monthlyTotals = new TreeMap<YearMonth, BigInteger>();
		for (var entry : expenses.findBetween(userId, earliest.atDay(1), current.plusMonths(1).atDay(1))) {
			YearMonth entryMonth = YearMonth.from(entry.date());
			BigInteger price = BigInteger.valueOf(entry.item().price());
			monthlyTotals.merge(entryMonth, price, BigInteger::add);
			if (entryMonth.equals(selected)) {
				categories.merge(entry.item().categoryCode(), price, BigInteger::add);
				dailyTotals.merge(entry.date(), price, BigInteger::add);
			}
		}

		BigInteger total = monthlyTotals.getOrDefault(selected, BigInteger.ZERO);
		return new MonthlyPattern(selected, total, differenceFromAverage(monthlyTotals, current, total),
				categories.entrySet().stream().filter(entry -> entry.getValue().signum() > 0)
						.map(entry -> new Category(entry.getKey(), entry.getValue(),
								new BigDecimal(entry.getValue()).divide(new BigDecimal(total), RATIO_SCALE, RoundingMode.HALF_UP)))
						.toList(),
				dailyTotals.entrySet().stream().map(entry -> new DailyTotal(entry.getKey(), entry.getValue())).toList());
	}

	@Override
	public DailyItems dailyItems(Long userId, LocalDate date) {
		validateMonth(YearMonth.from(date), YearMonth.now(clock));
		var items = expenses.findBetween(userId, date, date.plusDays(1)).stream()
				.map(LoadExpenseHistoryPort.Entry::item).toList();
		BigInteger total = items.stream().map(item -> BigInteger.valueOf(item.price()))
				.reduce(BigInteger.ZERO, BigInteger::add);
		return new DailyItems(date, total, items);
	}

	private static BigInteger differenceFromAverage(TreeMap<YearMonth, BigInteger> monthlyTotals,
			YearMonth current, BigInteger selectedTotal) {
		// API 22와 같이 기록이 있는 달만 사용하고 현재 달은 비교 기준에서 제외한다.
		var history = monthlyTotals.headMap(current).values();
		if (history.isEmpty()) {
			return null;
		}
		BigInteger historicalTotal = history.stream().reduce(BigInteger.ZERO, BigInteger::add);
		BigInteger average = new BigDecimal(historicalTotal)
				.divide(BigDecimal.valueOf(history.size()), 0, RoundingMode.HALF_UP).toBigIntegerExact();
		return selectedTotal.subtract(average);
	}

	private static YearMonth earliestMonth(YearMonth current) {
		return current.minusMonths(QUERY_MONTHS - 1);
	}

	private static void validateMonth(YearMonth month, YearMonth current) {
		if (month.isBefore(earliestMonth(current)) || month.isAfter(current)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "최근 6개월의 소비 기록만 조회할 수 있어요.");
		}
	}
}
