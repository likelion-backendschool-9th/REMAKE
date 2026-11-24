package com.remake.recipe.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.recipe.domain.CookTime;
import com.remake.recipe.domain.Cuisine;
import com.remake.recipe.domain.CustomOptions;
import com.remake.recipe.domain.Recipe;
import com.remake.recipe.domain.RecipeIngredient;
import com.remake.recipe.domain.RecipeSource;
import com.remake.recipe.domain.RecipeUnit;
import com.remake.recipe.domain.Taste;
import com.remake.support.IntegrationTestSupport;

// 레시피·재료·단계 세 테이블 저장과 jsonb(custom_options) 변환, 본인 소유 조회, 재료 삭제 시 SET NULL을 확인한다.
class RecipePersistenceAdapterTest extends IntegrationTestSupport {

	@Autowired
	private RecipePersistenceAdapter adapter;

	@Test
	void 커스텀_레시피를_저장하면_재료_순서와_단계_번호와_선택값이_그대로_읽힌다() {
		Long userId = createUser();
		Long eggId = createIngredient(userId, "DAIRY_EGG", "계란", LocalDate.of(2026, 3, 10));
		CustomOptions options = new CustomOptions(2, List.of(eggId), Taste.SPICY, CookTime.WITHIN_15, null, null,
				Cuisine.KOREAN, List.of("EGG"));
		Recipe recipe = Recipe.create(userId, RecipeSource.CUSTOM, "매운 계란말이", 2, 15, "약불로 익혀요.", options,
				List.of(new RecipeIngredient(eggId, "계란", new BigDecimal("3"), RecipeUnit.EA),
						new RecipeIngredient(null, "고춧가루", new BigDecimal("0.5"), RecipeUnit.TSP),
						new RecipeIngredient(null, "소금", null, RecipeUnit.TO_TASTE)),
				List.of("계란을 푼다.", "고춧가루를 넣는다.", "팬에 말아 익힌다."));

		Recipe saved = transactionTemplate.execute(status -> adapter.save(recipe));
		Recipe loaded = transactionTemplate
				.execute(status -> adapter.loadByIdAndUserId(saved.id(), userId).orElseThrow());

		assertThat(saved.id()).isNotNull();
		assertThat(loaded.userId()).isEqualTo(userId);
		assertThat(loaded.source()).isEqualTo(RecipeSource.CUSTOM);
		assertThat(loaded.title()).isEqualTo("매운 계란말이");
		assertThat(loaded.servings()).isEqualTo(2);
		assertThat(loaded.cookMinutes()).isEqualTo(15);
		assertThat(loaded.tips()).isEqualTo("약불로 익혀요.");
		assertThat(loaded.customOptions()).isEqualTo(options);
		assertThat(loaded.ingredients()).extracting(RecipeIngredient::name).containsExactly("계란", "고춧가루", "소금");
		assertThat(loaded.ingredients().get(0).ingredientId()).isEqualTo(eggId);
		assertThat(loaded.ingredients().get(1).amount()).isEqualByComparingTo("0.5");
		assertThat(loaded.ingredients().get(2).amount()).isNull();
		assertThat(loaded.ingredients().get(2).unit()).isEqualTo(RecipeUnit.TO_TASTE);
		assertThat(loaded.steps()).containsExactly("계란을 푼다.", "고춧가루를 넣는다.", "팬에 말아 익힌다.");
		assertThat(jdbcTemplate.queryForList("SELECT step_no FROM recipe_steps WHERE recipe_id = ? ORDER BY step_no",
				Integer.class, saved.id())).containsExactly(1, 2, 3);
		// 기본 시각 2026-03-04 10:00 (서울)
		assertThat(loaded.createdAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 4));
	}

	@Test
	void 빠른_생성은_선택값이_null로_저장된다() {
		Long userId = createUser();
		Recipe recipe = Recipe.create(userId, RecipeSource.QUICK, "계란프라이", 1, 5, null, null,
				List.of(new RecipeIngredient(null, "계란", BigDecimal.ONE, RecipeUnit.EA)), List.of("굽는다."));

		Recipe saved = transactionTemplate.execute(status -> adapter.save(recipe));

		assertThat(jdbcTemplate.queryForObject("SELECT custom_options IS NULL FROM recipes WHERE id = ?",
				Boolean.class, saved.id())).isTrue();
		assertThat(saved.customOptions()).isNull();
	}

	@Test
	void 다른_사용자_번호로는_읽히지_않는다() {
		Long ownerId = createUser();
		Long otherId = createUser();
		Recipe saved = transactionTemplate.execute(status -> adapter.save(Recipe.create(ownerId, RecipeSource.QUICK,
				"계란프라이", 1, 5, null, null, List.of(new RecipeIngredient(null, "계란", BigDecimal.ONE, RecipeUnit.EA)),
				List.of("굽는다."))));

		Boolean visible = transactionTemplate
				.execute(status -> adapter.loadByIdAndUserId(saved.id(), otherId).isPresent());

		assertThat(visible).isFalse();
	}

	@Test
	void 냉장고_재료를_지우면_레시피_재료의_연결만_끊긴다() {
		Long userId = createUser();
		Long eggId = createIngredient(userId, "DAIRY_EGG", "계란", LocalDate.of(2026, 3, 10));
		Recipe saved = transactionTemplate.execute(status -> adapter.save(Recipe.create(userId, RecipeSource.QUICK,
				"계란프라이", 1, 5, null, null, List.of(new RecipeIngredient(eggId, "계란", BigDecimal.ONE, RecipeUnit.EA)),
				List.of("굽는다."))));

		jdbcTemplate.update("DELETE FROM ingredients WHERE id = ?", eggId);
		Recipe loaded = transactionTemplate
				.execute(status -> adapter.loadByIdAndUserId(saved.id(), userId).orElseThrow());

		assertThat(loaded.ingredients()).singleElement().satisfies(ingredient -> {
			assertThat(ingredient.ingredientId()).isNull();
			assertThat(ingredient.name()).isEqualTo("계란");
		});
	}
}
