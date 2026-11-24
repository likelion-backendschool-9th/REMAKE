package com.remake.quota.domain;

import com.remake.user.domain.UserPlan;

/**
 * 요금제별 생성 한도(domain-rules "Plans and quota"). 한도는 기능마다 따로 적지 않고 여기 한 곳에만 둔다.
 *
 * @param recipeDaily     하루 레시피 생성 횟수
 * @param mealPlanMonthly 한 달 식단표 생성 횟수
 * @param mealPlanMaxDays 식단표 한 번에 만들 수 있는 최대 일수
 */
public record PlanQuota(int recipeDaily, int mealPlanMonthly, int mealPlanMaxDays) {

	public static final PlanQuota BASIC = new PlanQuota(2, 1, 5);
	public static final PlanQuota PREMIUM = new PlanQuota(4, 4, 7);

	public static PlanQuota of(UserPlan plan) {
		return plan == UserPlan.PREMIUM ? PREMIUM : BASIC;
	}
}
