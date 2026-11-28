package com.remake.receipt.adapter.in.web;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.remake.global.config.OpenApiConfig;
import com.remake.global.web.LoginUserId;
import com.remake.receipt.application.port.in.ExpenseHistoryUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "소비 패턴")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequestMapping("/api/v1/user/expenses")
@RequiredArgsConstructor
class ExpenseHistoryController {

	private final ExpenseHistoryUseCase history;

	@Operation(summary = "월별 소비 패턴")
	@GetMapping("/patterns")
	ExpenseHistoryUseCase.MonthlyPattern patterns(@LoginUserId Long userId,
			@RequestParam(required = false) YearMonth month) {
		return history.patterns(userId, month);
	}

	@Operation(summary = "일별 구매 재료")
	@GetMapping("/daily-items")
	ExpenseHistoryUseCase.DailyItems dailyItems(@LoginUserId Long userId, @RequestParam LocalDate date) {
		return history.dailyItems(userId, date);
	}

	@Operation(summary = "월별 폐기 재료")
	@GetMapping("/discarded-items")
	ExpenseHistoryUseCase.DiscardedItems discardedItems(@LoginUserId Long userId,
			@RequestParam(required = false) YearMonth month) {
		return history.discardedItems(userId, month);
	}
}
