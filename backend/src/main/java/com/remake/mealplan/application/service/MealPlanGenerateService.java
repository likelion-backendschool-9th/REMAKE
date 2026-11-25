package com.remake.mealplan.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.mealplan.application.port.in.GenerateMealPlanCommand;
import com.remake.mealplan.application.port.in.GenerateMealPlanUseCase;
import com.remake.mealplan.application.port.out.LoadMealPlanPort;
import com.remake.mealplan.application.port.out.LoadMealPlanSlotPort;
import com.remake.mealplan.domain.MealPlan;
import com.remake.mealplan.domain.MealPlanSlot;
import com.remake.quota.application.port.in.ConsumeMealPlanQuotaUseCase;
import com.remake.quota.application.port.in.GetUsageQuotasUseCase;
import com.remake.quota.domain.MealPlanQuota;

import lombok.RequiredArgsConstructor;

/**
 * 식단표 생성 접수(API 38).
 *
 * <p>이 메서드에는 트랜잭션을 걸지 않는다. 한도 초과로 거절할 때도 quota_exceeded(E07)가 남아야 하는데,
 * EventTracker는 트랜잭션이 롤백되면 이벤트를 보내지 않기 때문이다. 검사는 각 UseCase·Port가 자기 트랜잭션으로 읽고,
 * 저장과 작업 예약은 MealPlanRequestWriter가 한 트랜잭션에서 한다.
 */
@Service
@RequiredArgsConstructor
public class MealPlanGenerateService implements GenerateMealPlanUseCase {

	private final LoadMealPlanPort loadMealPlanPort;
	private final LoadMealPlanSlotPort loadMealPlanSlotPort;
	private final ConsumeMealPlanQuotaUseCase consumeMealPlanQuotaUseCase;
	private final GetUsageQuotasUseCase getUsageQuotasUseCase;
	private final MealPlanRequestWriter requestWriter;
	private final EventTracker eventTracker;
	private final Clock clock;

	@Override
	public MealPlan generate(GenerateMealPlanCommand command) {
		Instant acceptedAt = clock.instant();
		LocalDate today = LocalDate.now(clock);
		Long userId = command.userId();

		if (command.startDate().isBefore(today)) {
			throw new BusinessException(ErrorCode.PAST_DATE_NOT_ALLOWED);
		}
		if (loadMealPlanPort.existsGenerating(userId)) {
			throw new BusinessException(ErrorCode.MEAL_PLAN_IN_PROGRESS);
		}
		MealPlanQuota quota = requireQuota(userId);
		List<LocalDate> overlapping = overlappingDates(userId, command.startDate(), quota.maxDays());
		if (!overlapping.isEmpty() && !command.overwrite()) {
			throw new BusinessException(ErrorCode.MEAL_PLAN_OVERLAP, ErrorCode.MEAL_PLAN_OVERLAP.getMessage(),
					Map.of("overlappingDates", overlapping));
		}

		MealPlan request = MealPlan.request(userId, command.startDate(), command.purpose(), command.mealTypes(),
				command.servings(), today);
		return requestWriter.saveAndRequest(request, quota.maxDays(), command.overwrite(), acceptedAt);
	}

	/**
	 * 시작일부터 최대 일수 안에 내 식단 칸이 있는 날(날짜 순, 중복 없음).
	 * 칸은 완성된 식단표에만 있으므로 생성 중·실패 식단표는 겹침에 들어가지 않는다(#144 제안 A와 같은 결과).
	 */
	private List<LocalDate> overlappingDates(Long userId, LocalDate startDate, int maxDays) {
		return loadMealPlanSlotPort.loadUserSlotsBetween(userId, startDate, startDate.plusDays(maxDays - 1L))
				.stream()
				.map(MealPlanSlot::slotDate)
				.distinct()
				.sorted()
				.toList();
	}

	/** 이번 달 횟수가 남았는지. 다 썼으면 quota_exceeded(E07)를 남기고 MEAL_PLAN_LIMIT_EXCEEDED를 그대로 던진다. */
	private MealPlanQuota requireQuota(Long userId) {
		try {
			return consumeMealPlanQuotaUseCase.requireMealPlanQuota(userId);
		} catch (BusinessException e) {
			if (e.getErrorCode() == ErrorCode.MEAL_PLAN_LIMIT_EXCEEDED) {
				trackQuotaExceeded(userId, getUsageQuotasUseCase.getUsageQuotas(userId).mealPlan());
			}
			throw e;
		}
	}

	private void trackQuotaExceeded(Long userId, MealPlanQuota quota) {
		eventTracker.track(EventName.QUOTA_EXCEEDED, Screen.MEAL_PLAN_SETUP, Map.of(
				"limit_feature", "MEAL_PLAN",
				"limit_total", quota.limit(),
				"used_count", quota.used()),
				EventTrackCommand.ofUser(userId));
	}
}
