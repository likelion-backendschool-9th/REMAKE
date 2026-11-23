package com.remake.recipe.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI가 만든 레시피. 만든 뒤에는 고치지 않는다. Spring·JPA 어노테이션을 붙이지 않는다.
 * 규칙을 어기면 IllegalArgumentException을 던진다. 재료 순서와 단계 번호는 목록 순서를 따른다(단계는 1번부터).
 */
public class Recipe {

	public static final int TITLE_MAX_LENGTH = 100;
	public static final int TIPS_MAX_LENGTH = 1000;
	public static final int STEP_MAX_LENGTH = 1000;

	private final Long id;
	private final Long userId;
	private final RecipeSource source;
	private final String title;
	private final int servings;
	private final int cookMinutes;
	private final String tips;
	private final CustomOptions customOptions;
	private final List<RecipeIngredient> ingredients;
	private final List<String> steps;
	private final LocalDateTime createdAt;

	private Recipe(Long id, Long userId, RecipeSource source, String title, int servings, int cookMinutes,
			String tips, CustomOptions customOptions, List<RecipeIngredient> ingredients, List<String> steps,
			LocalDateTime createdAt) {
		this.id = id;
		this.userId = userId;
		this.source = source;
		this.title = title;
		this.servings = servings;
		this.cookMinutes = cookMinutes;
		this.tips = tips;
		this.customOptions = customOptions;
		this.ingredients = List.copyOf(ingredients);
		this.steps = List.copyOf(steps);
		this.createdAt = createdAt;
	}

	/**
	 * 새 레시피를 만든다. 커스텀 생성(CUSTOM)만 customOptions가 있고 다른 경로는 null이다.
	 * 재료와 단계는 하나 이상 있어야 한다. 팁은 비어 있으면 null로 둔다.
	 */
	public static Recipe create(Long userId, RecipeSource source, String title, int servings, int cookMinutes,
			String tips, CustomOptions customOptions, List<RecipeIngredient> ingredients, List<String> steps) {
		if (userId == null) {
			throw new IllegalArgumentException("사용자는 필수입니다.");
		}
		if (source == null) {
			throw new IllegalArgumentException("레시피 생성 경로는 필수입니다.");
		}
		if ((source == RecipeSource.CUSTOM) != (customOptions != null)) {
			throw new IllegalArgumentException("커스텀 생성만 선택값을 가집니다.");
		}
		if (servings <= 0) {
			throw new IllegalArgumentException("인분은 1 이상이어야 합니다.");
		}
		if (cookMinutes <= 0) {
			throw new IllegalArgumentException("조리 시간은 1분 이상이어야 합니다.");
		}
		if (ingredients == null || ingredients.isEmpty()) {
			throw new IllegalArgumentException("레시피 재료가 하나 이상 있어야 합니다.");
		}
		return new Recipe(null, userId, source, normalizeTitle(title), servings, cookMinutes, normalizeTips(tips),
				customOptions, ingredients, normalizeSteps(steps), null);
	}

	/** 저장된 값으로 되살린다. 저장소 어댑터만 쓴다. 이미 저장된 값이라 규칙을 다시 확인하지 않는다. */
	public static Recipe restore(Long id, Long userId, RecipeSource source, String title, int servings,
			int cookMinutes, String tips, CustomOptions customOptions, List<RecipeIngredient> ingredients,
			List<String> steps, LocalDateTime createdAt) {
		return new Recipe(id, userId, source, title, servings, cookMinutes, tips, customOptions, ingredients, steps,
				createdAt);
	}

	private static String normalizeTitle(String title) {
		if (title == null || title.isBlank()) {
			throw new IllegalArgumentException("레시피 이름은 비워 둘 수 없습니다.");
		}
		String trimmed = title.strip();
		if (trimmed.length() > TITLE_MAX_LENGTH) {
			throw new IllegalArgumentException("레시피 이름은 " + TITLE_MAX_LENGTH + "자 이내여야 합니다.");
		}
		return trimmed;
	}

	private static String normalizeTips(String tips) {
		if (tips == null || tips.isBlank()) {
			return null;
		}
		String trimmed = tips.strip();
		if (trimmed.length() > TIPS_MAX_LENGTH) {
			throw new IllegalArgumentException("팁은 " + TIPS_MAX_LENGTH + "자 이내여야 합니다.");
		}
		return trimmed;
	}

	private static List<String> normalizeSteps(List<String> steps) {
		if (steps == null || steps.isEmpty()) {
			throw new IllegalArgumentException("조리 단계가 하나 이상 있어야 합니다.");
		}
		return steps.stream().map(Recipe::normalizeStep).toList();
	}

	private static String normalizeStep(String step) {
		if (step == null || step.isBlank()) {
			throw new IllegalArgumentException("조리 단계는 비워 둘 수 없습니다.");
		}
		String trimmed = step.strip();
		if (trimmed.length() > STEP_MAX_LENGTH) {
			throw new IllegalArgumentException("조리 단계는 " + STEP_MAX_LENGTH + "자 이내여야 합니다.");
		}
		return trimmed;
	}

	public Long id() {
		return id;
	}

	public Long userId() {
		return userId;
	}

	public RecipeSource source() {
		return source;
	}

	public String title() {
		return title;
	}

	public int servings() {
		return servings;
	}

	public int cookMinutes() {
		return cookMinutes;
	}

	public String tips() {
		return tips;
	}

	public CustomOptions customOptions() {
		return customOptions;
	}

	/** 화면 순서대로의 재료 */
	public List<RecipeIngredient> ingredients() {
		return ingredients;
	}

	/** 단계 내용. 목록의 0번이 1단계다. */
	public List<String> steps() {
		return steps;
	}

	/** 저장 전에는 null */
	public LocalDateTime createdAt() {
		return createdAt;
	}
}
