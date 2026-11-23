package com.remake.quota.application.port.in;

import com.remake.quota.domain.MealPlanQuota;
import com.remake.quota.domain.RecipeQuota;

/** 남은 생성 횟수 조회(API 27). 레시피는 오늘(한국 시간 00:00부터), 식단표는 이번 달(1일 00:00부터) 기준이다. */
public interface GetUsageQuotasUseCase {

	UsageQuotas getUsageQuotas(Long userId);

	RecipeQuota getRecipeQuota(Long userId);

	record UsageQuotas(RecipeQuota recipe, MealPlanQuota mealPlan) {
	}
}
