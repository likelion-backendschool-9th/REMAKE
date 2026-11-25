package com.remake.mealplan.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 식단 칸 하나(날짜·끼니마다 레시피 하나). Spring·JPA 어노테이션을 붙이지 않는다.
 * 규칙을 어기면 IllegalArgumentException을 던진다. 현재 시각은 읽지 않는다. 필요한 날짜·시각은 인수로 받는다.
 */
public class MealPlanSlot {

	private final Long id;
	private final Long mealPlanId;
	private final LocalDate slotDate;
	private final MealType mealType;

	private Long recipeId;
	private LocalDateTime cookedAt;

	private MealPlanSlot(Long id, Long mealPlanId, LocalDate slotDate, MealType mealType, Long recipeId,
			LocalDateTime cookedAt) {
		this.id = id;
		this.mealPlanId = mealPlanId;
		this.slotDate = slotDate;
		this.mealType = mealType;
		this.recipeId = recipeId;
		this.cookedAt = cookedAt;
	}

	/** 생성 작업이 만든 레시피로 칸을 채운다. 칸에는 항상 레시피가 있다. */
	public static MealPlanSlot create(Long mealPlanId, LocalDate slotDate, MealType mealType, Long recipeId) {
		requireNonNull(mealPlanId, "식단표");
		requireNonNull(slotDate, "날짜");
		requireNonNull(mealType, "끼니");
		requireNonNull(recipeId, "레시피");
		return new MealPlanSlot(null, mealPlanId, slotDate, mealType, recipeId, null);
	}

	/** 저장된 값으로 되살린다. 저장소 어댑터만 쓴다. 이미 저장된 값이라 규칙을 다시 확인하지 않는다. */
	public static MealPlanSlot restore(Long id, Long mealPlanId, LocalDate slotDate, MealType mealType, Long recipeId,
			LocalDateTime cookedAt) {
		return new MealPlanSlot(id, mealPlanId, slotDate, mealType, recipeId, cookedAt);
	}

	/** 지난 날짜의 칸인지. 지난 칸은 삭제·교체할 수 없다(PAST_DATE_NOT_ALLOWED는 서비스가 낸다). */
	public boolean isPast(LocalDate today) {
		return slotDate.isBefore(today);
	}

	/**
	 * 레시피 교체: 새로 만든 레시피로 바꾼다. 지난 칸인지는 서비스가 먼저 확인한다.
	 * 요리 완료 표시는 지운다(요리한 것은 바꾸기 전 레시피라 새 레시피는 아직 요리 전이다, #176 11-25 결정).
	 */
	public void replaceRecipe(Long newRecipeId) {
		requireNonNull(newRecipeId, "레시피");
		this.recipeId = newRecipeId;
		this.cookedAt = null;
	}

	/** 요리 완료 표시. 오늘 칸에서 연 레시피로 요리했을 때만 기록한다(다른 날 칸은 안 됨). */
	public void markCooked(LocalDateTime cookedAt) {
		requireNonNull(cookedAt, "요리 시각");
		if (!cookedAt.toLocalDate().equals(slotDate)) {
			throw new IllegalArgumentException("오늘 칸만 요리 완료로 표시할 수 있습니다.");
		}
		this.cookedAt = cookedAt;
	}

	/** 요리한 칸인지. 요리한 칸은 새 식단표로 덮어써도 남는다. */
	public boolean isCooked() {
		return cookedAt != null;
	}

	private static void requireNonNull(Object value, String name) {
		if (value == null) {
			throw new IllegalArgumentException(name + " 값이 필요합니다.");
		}
	}

	public Long id() {
		return id;
	}

	public Long mealPlanId() {
		return mealPlanId;
	}

	public LocalDate slotDate() {
		return slotDate;
	}

	public MealType mealType() {
		return mealType;
	}

	public Long recipeId() {
		return recipeId;
	}

	public LocalDateTime cookedAt() {
		return cookedAt;
	}
}
