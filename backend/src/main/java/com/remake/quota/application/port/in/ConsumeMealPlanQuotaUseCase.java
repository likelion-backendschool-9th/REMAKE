package com.remake.quota.application.port.in;

import com.remake.quota.domain.MealPlanQuota;

/**
 * 식단표 생성 횟수 확인·차감(이번 달 기준). 레시피의 {@link ConsumeRecipeQuotaUseCase}와 같은 방식이다.
 * <ol>
 * <li>생성 접수(API 38)에서 {@link #requireMealPlanQuota}로 남은 횟수를 확인한다(잠그지 않음)</li>
 * <li>서버 작업이 성공하면 식단표를 저장하는 트랜잭션 안에서 {@link #consumeMealPlanQuota}를 부른다</li>
 * </ol>
 * 실패(재료 부족·AI 오류·시간 초과)는 차감하지 않으므로 consume을 부르지 않는다.
 */
public interface ConsumeMealPlanQuotaUseCase {

	/** 남은 횟수가 없으면 MEAL_PLAN_LIMIT_EXCEEDED를 던진다. 있으면 현재 횟수(최대 일수 포함)를 돌려준다. */
	MealPlanQuota requireMealPlanQuota(Long userId);

	/**
	 * 호출한 쪽의 트랜잭션에 참여해(REQUIRED) 회원 단위로 잠그고 이번 달 횟수를 다시 센 뒤 usage_logs에 MEAL_PLAN 한 행을 넣는다.
	 * 그 사이 횟수를 다 썼으면 MEAL_PLAN_LIMIT_EXCEEDED를 던지고, 호출한 쪽 트랜잭션이 롤백되어 식단표도 저장되지 않는다.
	 * 차감 뒤의 횟수를 돌려준다.
	 */
	MealPlanQuota consumeMealPlanQuota(Long userId);
}
