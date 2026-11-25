package com.remake.mealplan.application.port.out;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.remake.mealplan.domain.MealPlan;
import com.remake.mealplan.domain.MealPlanStatus;

public interface LoadMealPlanPort {

	/** 회원의 식단표 중 GENERATING이 있는지. */
	boolean existsGenerating(Long userId);

	Optional<MealPlan> loadById(Long mealPlanId);

	/** 회원의 식단표 중 그 상태이고 시작일이 from~to(둘 다 포함)인 것. 시작일·id 순. */
	List<MealPlan> loadByStatusStartingBetween(Long userId, MealPlanStatus status, LocalDate from, LocalDate to);

	/**
	 * 식단표 행을 잠그고 읽는다(SELECT … FOR UPDATE). 트랜잭션 안에서만 부른다.
	 * 상태를 바꾸는 쪽(완성·실패·끊긴 작업 정리)이 모두 이걸로 읽어서, 같은 식단표를 동시에 끝내지 않게 한다.
	 */
	Optional<MealPlan> loadByIdForUpdate(Long mealPlanId);

	/** 이 시각 전에 접수되었는데 아직 GENERATING인 식단표(끊긴 작업 후보). 접수 순. */
	List<GeneratingMealPlan> loadGeneratingAcceptedBefore(LocalDateTime acceptedBefore);

	/** @param acceptedAt 접수 시각(meal_plans.created_at) */
	record GeneratingMealPlan(Long mealPlanId, LocalDateTime acceptedAt) {
	}
}
