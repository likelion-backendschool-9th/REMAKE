package com.remake.quota.adapter.in.web;

import com.remake.quota.application.port.in.GetUsageQuotasUseCase.UsageQuotas;
import com.remake.quota.domain.MealPlanQuota;
import com.remake.quota.domain.RecipeQuota;

/** API 27 응답. { recipe: { limit, used, bonusRemaining, remaining }, mealPlan: { limit, used, remaining, maxDays } } */
public record UsageQuotaResponse(Recipe recipe, MealPlan mealPlan) {

	/**
	 * @param limit          하루 기본 횟수(일반 2, 구독 4)
	 * @param used           오늘 쓴 횟수
	 * @param bonusRemaining 남은 보상 횟수
	 * @param remaining      지금 생성할 수 있는 횟수(남은 기본 + 보상)
	 */
	public record Recipe(int limit, int used, int bonusRemaining, int remaining) {
	}

	/**
	 * @param limit     이번 달 횟수(일반 1, 구독 4)
	 * @param used      이번 달 쓴 횟수
	 * @param remaining 남은 횟수
	 * @param maxDays   한 번에 만들 수 있는 최대 일수(일반 5, 구독 7)
	 */
	public record MealPlan(int limit, int used, int remaining, int maxDays) {
	}

	static UsageQuotaResponse from(UsageQuotas quotas) {
		RecipeQuota recipe = quotas.recipe();
		MealPlanQuota mealPlan = quotas.mealPlan();
		return new UsageQuotaResponse(
				new Recipe(recipe.limit(), recipe.used(), recipe.bonusRemaining(), recipe.remaining()),
				new MealPlan(mealPlan.limit(), mealPlan.used(), mealPlan.remaining(), mealPlan.maxDays()));
	}
}
