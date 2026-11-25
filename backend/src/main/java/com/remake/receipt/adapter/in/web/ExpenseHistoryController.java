package com.remake.receipt.adapter.in.web;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.remake.global.web.LoginUserId;
import com.remake.receipt.application.port.in.ExpenseHistoryUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/user/expenses")
@RequiredArgsConstructor
class ExpenseHistoryController {

	private final ExpenseHistoryUseCase history;

	@GetMapping("/patterns")
	ExpenseHistoryUseCase.MonthlyPattern patterns(@LoginUserId Long userId,
			@RequestParam(required = false) YearMonth month) {
		return history.patterns(userId, month);
	}

	@GetMapping("/daily-items")
	ExpenseHistoryUseCase.DailyItems dailyItems(@LoginUserId Long userId, @RequestParam LocalDate date) {
		return history.dailyItems(userId, date);
	}
}
