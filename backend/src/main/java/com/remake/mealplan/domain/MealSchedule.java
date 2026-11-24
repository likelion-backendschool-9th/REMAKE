package com.remake.mealplan.domain;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 이번 생성에서 채울 칸들. 시작일부터 days일 동안 고른 끼니를 채운다.
 * keptSlots는 덮어쓰기에서 남기는 칸(오늘 요리한 칸)이라 새로 채우지 않는다.
 */
public record MealSchedule(LocalDate startDate, int days, Set<MealType> mealTypes, Set<MealSlotKey> keptSlots) {

	public MealSchedule {
		if (startDate == null) {
			throw new IllegalArgumentException("시작일이 필요합니다.");
		}
		if (days < 1 || days > MealPlan.MAX_DAYS) {
			throw new IllegalArgumentException("일수는 1~" + MealPlan.MAX_DAYS + "일이어야 합니다.");
		}
		if (mealTypes == null || mealTypes.isEmpty()) {
			throw new IllegalArgumentException("끼니를 1개 이상 골라야 합니다.");
		}
		mealTypes = Collections.unmodifiableSet(EnumSet.copyOf(mealTypes));
		keptSlots = keptSlots == null ? Set.of() : Set.copyOf(keptSlots);
	}

	/** 시작일부터 날짜 순. */
	public List<LocalDate> dates() {
		return startDate.datesUntil(startDate.plusDays(days)).toList();
	}

	/** 그날 새로 채울 끼니(남기는 칸 제외). 남기는 칸만 있는 날이면 비어 있다. */
	public Set<MealType> mealTypesOn(LocalDate date) {
		return mealTypes.stream()
				.filter(mealType -> !keptSlots.contains(new MealSlotKey(date, mealType)))
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(MealType.class)));
	}

	public boolean contains(LocalDate date) {
		return !date.isBefore(startDate) && date.isBefore(startDate.plusDays(days));
	}
}
