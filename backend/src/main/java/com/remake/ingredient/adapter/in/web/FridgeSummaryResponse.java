package com.remake.ingredient.adapter.in.web;

import com.remake.ingredient.domain.FridgeSummary;

/** 냉장고 요약. total = danger + soon + ok */
public record FridgeSummaryResponse(int total, int danger, int soon, int ok) {

	public static FridgeSummaryResponse from(FridgeSummary summary) {
		return new FridgeSummaryResponse(summary.total(), summary.danger(), summary.soon(), summary.ok());
	}
}
