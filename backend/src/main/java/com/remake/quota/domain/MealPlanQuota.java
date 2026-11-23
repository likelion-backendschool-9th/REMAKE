package com.remake.quota.domain;

/**
 * 이번 달 식단표 생성 횟수.
 *
 * @param limit   요금제의 한 달 횟수
 * @param used    이번 달 성공한 생성 수
 * @param maxDays 한 번에 만들 수 있는 최대 일수
 */
public record MealPlanQuota(int limit, int used, int maxDays) {

	public int remaining() {
		return Math.max(limit - used, 0);
	}
}
