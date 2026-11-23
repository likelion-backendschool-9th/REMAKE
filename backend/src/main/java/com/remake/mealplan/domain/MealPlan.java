package com.remake.mealplan.domain;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * 식단표 한 건(생성 요청과 그 결과). Spring·JPA 어노테이션을 붙이지 않는다.
 * 규칙을 어기면 IllegalArgumentException, 상태 순서를 어기면 IllegalStateException을 던진다.
 * 현재 시각은 읽지 않는다. 오늘 날짜는 인수로 받는다.
 */
public class MealPlan {

	/** 한 번에 만들 수 있는 최대 일수(구독 7일). 요금제별 최대 일수는 횟수 기능이 정한다. */
	public static final int MAX_DAYS = 7;

	private final Long id;
	private final Long userId;
	private final LocalDate startDate;
	private final MealPurpose purpose;
	private final Set<MealType> mealTypes;
	private final int servings;

	private Integer generatedDays;
	private MealPlanStatus status;

	private MealPlan(Long id, Long userId, LocalDate startDate, MealPurpose purpose, Set<MealType> mealTypes,
			int servings, Integer generatedDays, MealPlanStatus status) {
		this.id = id;
		this.userId = userId;
		this.startDate = startDate;
		this.purpose = purpose;
		this.mealTypes = Collections.unmodifiableSet(EnumSet.copyOf(mealTypes));
		this.servings = servings;
		this.generatedDays = generatedDays;
		this.status = status;
	}

	/** 생성 요청을 받는다. 시작일은 오늘부터, 끼니는 1개 이상, 인원은 1명 이상이다. 상태는 GENERATING이다. */
	public static MealPlan request(Long userId, LocalDate startDate, MealPurpose purpose, Set<MealType> mealTypes,
			int servings, LocalDate today) {
		requireNonNull(userId, "사용자");
		requireNonNull(startDate, "시작일");
		requireNonNull(purpose, "식단 목적");
		requireNonNull(today, "오늘 날짜");
		if (startDate.isBefore(today)) {
			throw new IllegalArgumentException("시작일은 오늘부터 고를 수 있습니다.");
		}
		if (mealTypes == null || mealTypes.isEmpty()) {
			throw new IllegalArgumentException("끼니를 1개 이상 골라야 합니다.");
		}
		if (servings < 1) {
			throw new IllegalArgumentException("인원은 1명 이상이어야 합니다.");
		}
		return new MealPlan(null, userId, startDate, purpose, mealTypes, servings, null, MealPlanStatus.GENERATING);
	}

	/** 저장된 값으로 되살린다. 저장소 어댑터만 쓴다. 이미 저장된 값이라 규칙을 다시 확인하지 않는다. */
	public static MealPlan restore(Long id, Long userId, LocalDate startDate, MealPurpose purpose,
			Set<MealType> mealTypes, int servings, Integer generatedDays, MealPlanStatus status) {
		return new MealPlan(id, userId, startDate, purpose, mealTypes, servings, generatedDays, status);
	}

	/** 서버 작업이 성공했다. 실제로 만든 일수(1~7)를 기록한다. */
	public void complete(int generatedDays) {
		requireGenerating();
		if (generatedDays < 1 || generatedDays > MAX_DAYS) {
			throw new IllegalArgumentException("만든 일수는 1~" + MAX_DAYS + "일이어야 합니다.");
		}
		this.generatedDays = generatedDays;
		this.status = MealPlanStatus.COMPLETED;
	}

	/** 서버 작업이 실패했다(재료 부족, AI 오류, 시간 초과). 실패 사유는 저장하지 않는다. */
	public void fail() {
		requireGenerating();
		this.status = MealPlanStatus.FAILED;
	}

	/** 식단표의 마지막 날. 완성된 식단표만 있다. */
	public Optional<LocalDate> endDate() {
		if (generatedDays == null) {
			return Optional.empty();
		}
		return Optional.of(startDate.plusDays(generatedDays - 1L));
	}

	private void requireGenerating() {
		if (status != MealPlanStatus.GENERATING) {
			throw new IllegalStateException("생성 중인 식단표만 끝낼 수 있습니다. 지금 상태: " + status);
		}
	}

	private static void requireNonNull(Object value, String name) {
		if (value == null) {
			throw new IllegalArgumentException(name + " 값이 필요합니다.");
		}
	}

	public Long id() {
		return id;
	}

	public Long userId() {
		return userId;
	}

	public LocalDate startDate() {
		return startDate;
	}

	public MealPurpose purpose() {
		return purpose;
	}

	public Set<MealType> mealTypes() {
		return mealTypes;
	}

	public int servings() {
		return servings;
	}

	public Integer generatedDays() {
		return generatedDays;
	}

	public MealPlanStatus status() {
		return status;
	}
}
