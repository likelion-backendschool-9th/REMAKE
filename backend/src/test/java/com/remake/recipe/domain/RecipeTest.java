package com.remake.recipe.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class RecipeTest {

	private static final List<RecipeIngredient> INGREDIENTS = List.of(
			new RecipeIngredient(1L, "계란", new BigDecimal("2"), RecipeUnit.EA));
	private static final List<String> STEPS = List.of("계란을 푼다.", "팬에 굽는다.");

	@Test
	void 이름_팁_단계의_앞뒤_공백을_지우고_빈_팁은_null로_둔다() {
		Recipe recipe = Recipe.create(1L, RecipeSource.QUICK, "  계란말이 ", 1, 10, "   ", null, INGREDIENTS,
				List.of(" 계란을 푼다. ", "팬에 굽는다."));

		assertThat(recipe.title()).isEqualTo("계란말이");
		assertThat(recipe.tips()).isNull();
		assertThat(recipe.steps()).containsExactly("계란을 푼다.", "팬에 굽는다.");
		assertThat(recipe.id()).isNull();
	}

	@Test
	void 커스텀_생성만_선택값을_가진다() {
		CustomOptions options = new CustomOptions(2, List.of(1L), Taste.SPICY, null, null, null, null, null);

		assertThatThrownBy(() -> Recipe.create(1L, RecipeSource.QUICK, "계란말이", 1, 10, null, options,
				INGREDIENTS, STEPS)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Recipe.create(1L, RecipeSource.CUSTOM, "계란말이", 1, 10, null, null,
				INGREDIENTS, STEPS)).isInstanceOf(IllegalArgumentException.class);
		assertThat(Recipe.create(1L, RecipeSource.CUSTOM, "계란말이", 2, 10, null, options, INGREDIENTS, STEPS)
				.customOptions()).isEqualTo(options);
	}

	@Test
	void 재료나_단계가_없으면_만들_수_없다() {
		assertThatThrownBy(() -> Recipe.create(1L, RecipeSource.QUICK, "계란말이", 1, 10, null, null, List.of(),
				STEPS)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Recipe.create(1L, RecipeSource.QUICK, "계란말이", 1, 10, null, null, INGREDIENTS,
				List.of())).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Recipe.create(1L, RecipeSource.QUICK, "계란말이", 1, 10, null, null, INGREDIENTS,
				List.of("계란을 푼다.", " "))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 인분과_조리_시간은_1_이상이어야_한다() {
		assertThatThrownBy(() -> Recipe.create(1L, RecipeSource.QUICK, "계란말이", 0, 10, null, null, INGREDIENTS,
				STEPS)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Recipe.create(1L, RecipeSource.QUICK, "계란말이", 1, 0, null, null, INGREDIENTS,
				STEPS)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 재료_양은_비워_둘_수_있지만_0_이하나_소수_넷째_자리는_안_된다() {
		assertThat(new RecipeIngredient(null, "소금", null, RecipeUnit.TO_TASTE).amount()).isNull();
		assertThatThrownBy(() -> new RecipeIngredient(null, "소금", BigDecimal.ZERO, RecipeUnit.G))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new RecipeIngredient(null, "소금", new BigDecimal("0.0001"), RecipeUnit.G))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new RecipeIngredient(null, " ", BigDecimal.ONE, RecipeUnit.G))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 커스텀_선택값의_빈_목록은_빈_목록으로_둔다() {
		CustomOptions options = new CustomOptions(1, null, null, null, null, null, null, null);

		assertThat(options.ingredientIds()).isEmpty();
		assertThat(options.allergenCodes()).isEmpty();
		assertThatThrownBy(() -> new CustomOptions(0, null, null, null, null, null, null, null))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
