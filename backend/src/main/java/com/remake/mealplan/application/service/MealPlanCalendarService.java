package com.remake.mealplan.application.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.mealplan.application.port.in.GetMealPlanCalendarUseCase;
import com.remake.mealplan.application.port.in.MealPlanCalendar;
import com.remake.mealplan.application.port.out.LoadMealPlanPort;
import com.remake.mealplan.application.port.out.LoadMealPlanSlotPort;
import com.remake.mealplan.domain.MealPlan;
import com.remake.mealplan.domain.MealPlanSlot;
import com.remake.mealplan.domain.MealPlanStatus;

import lombok.RequiredArgsConstructor;

/**
 * 월 달력(API 39). 기간은 완성된(COMPLETED) 식단표만 보여 준다.
 * 생성 중·실패 식단표는 끝 날짜가 없고 칸도 없어 넣지 않는다(#144 준호 님 제안 A, 결정되면 CALENDAR_STATUS만 바꾼다).
 * hasAnyMealPlan(빈 화면 조건)도 같은 상태로 센다: 첫 식단표를 만드는 중이면 아직 false다.
 */
@Service
@RequiredArgsConstructor
public class MealPlanCalendarService implements GetMealPlanCalendarUseCase {

	/** 달력에 기간을 보여 줄 식단표 상태(#144 미결, 제안 A). */
	static final MealPlanStatus CALENDAR_STATUS = MealPlanStatus.COMPLETED;

	private final LoadMealPlanPort loadMealPlanPort;
	private final LoadMealPlanSlotPort loadMealPlanSlotPort;

	@Override
	@Transactional(readOnly = true)
	public MealPlanCalendar getCalendar(Long userId, YearMonth month) {
		LocalDate first = month.atDay(1);
		LocalDate last = month.atEndOfMonth();

		// 지난달 말에 시작해 이번 달로 이어지는 식단표도 넣는다(가장 긴 식단표가 MAX_DAYS일이다)
		List<MealPlanCalendar.Period> periods = loadMealPlanPort
				.loadByStatusStartingBetween(userId, CALENDAR_STATUS, first.minusDays(MealPlan.MAX_DAYS - 1L), last)
				.stream()
				.filter(plan -> plan.endDate().filter(end -> !end.isBefore(first)).isPresent())
				.map(plan -> new MealPlanCalendar.Period(plan.id(), plan.startDate(), plan.endDate().orElseThrow()))
				.toList();
		List<LocalDate> datesWithMeals = loadMealPlanSlotPort.loadUserSlotsBetween(userId, first, last).stream()
				.map(MealPlanSlot::slotDate)
				.distinct()
				.sorted()
				.toList();
		boolean hasAnyMealPlan = !periods.isEmpty() || loadMealPlanPort.existsWithStatus(userId, CALENDAR_STATUS);
		return new MealPlanCalendar(month, hasAnyMealPlan, periods, datesWithMeals);
	}
}
