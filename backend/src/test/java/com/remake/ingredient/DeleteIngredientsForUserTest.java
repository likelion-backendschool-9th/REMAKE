package com.remake.ingredient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.ingredient.application.port.in.ConsumeIngredientCommand;
import com.remake.ingredient.application.port.in.ConsumeIngredientUseCase;
import com.remake.ingredient.application.port.in.DeleteIngredientsForUserUseCase;
import com.remake.ingredient.domain.ConsumeVia;
import com.remake.ingredient.domain.IngredientLogType;
import com.remake.support.IntegrationTestSupport;

// 탈퇴 때 냉장고 재료와 사용·폐기 기록을 회원 단위로 지우는 UseCase(#246). 오늘은 시험용 시계의 2026-03-04이다.
class DeleteIngredientsForUserTest extends IntegrationTestSupport {

	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	private Long me;
	private Long other;

	@Autowired
	private DeleteIngredientsForUserUseCase deleteUseCase;

	@Autowired
	private ConsumeIngredientUseCase consumeUseCase;

	@BeforeEach
	void setUpUsers() {
		me = createUser();
		other = createUser();
	}

	// 재료 하나를 만들고 일부를 사용해 기록을 남긴다(재료 1개 + 기록 1개)
	private Long ingredientWithLog(Long userId, String name) {
		Long id = createIngredient(userId, "VEGETABLE", name, TODAY.plusDays(5));
		jdbcTemplate.update("UPDATE ingredients SET amount = 4, initial_amount = 4 WHERE id = ?", id);
		consumeUseCase.consumeIngredient(
				new ConsumeIngredientCommand(userId, id, IngredientLogType.USED, BigDecimal.ONE, ConsumeVia.CONSUME));
		return id;
	}

	private long count(String table, Long userId) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table + " WHERE user_id = ?", Long.class, userId);
	}

	@Test
	void 해당_회원의_재료와_기록만_지워지고_다른_회원_것은_남는다() {
		ingredientWithLog(me, "양파");
		ingredientWithLog(me, "대파");
		ingredientWithLog(other, "당근");

		deleteUseCase.deleteIngredientsForUser(me);

		assertThat(count("ingredients", me)).isZero();
		assertThat(count("ingredient_logs", me)).isZero();
		assertThat(count("ingredients", other)).isEqualTo(1);
		assertThat(count("ingredient_logs", other)).isEqualTo(1);
	}

	@Test
	void 재료가_없는_회원도_오류_없이_끝난다() {
		ingredientWithLog(other, "당근");

		assertThatCode(() -> deleteUseCase.deleteIngredientsForUser(me)).doesNotThrowAnyException();

		assertThat(count("ingredients", other)).isEqualTo(1);
	}

	@Test
	void 이미_다_써서_재료는_없고_기록만_남은_회원의_기록도_지워진다() {
		Long id = createIngredient(me, "VEGETABLE", "양파", TODAY.plusDays(5));
		consumeUseCase.consumeIngredient(
				new ConsumeIngredientCommand(me, id, IngredientLogType.DISCARDED, BigDecimal.ONE, ConsumeVia.CONSUME));
		assertThat(count("ingredients", me)).isZero();
		assertThat(count("ingredient_logs", me)).isEqualTo(1);

		deleteUseCase.deleteIngredientsForUser(me);

		assertThat(count("ingredient_logs", me)).isZero();
	}

	@Test
	void 재료를_참조하던_레시피_재료와_알림은_행이_남고_연결만_비워진다() {
		Long id = ingredientWithLog(me, "양파");
		// 레시피 재료·알림은 다른 회원(other) 것으로 만든다. 탈퇴 회원 본인의 알림은 알림 쪽 삭제(#237)가 지우므로,
		// 여기서는 "재료 삭제가 남의 행을 지우지 않고 연결만 비운다"만 확인한다.
		Long recipeId = createRecipe(other);
		jdbcTemplate.update("""
				INSERT INTO recipe_ingredients (recipe_id, ingredient_id, name, amount, unit, sort_order, created_at)
				VALUES (?, ?, '양파', 1, 'EA', 0, ?)
				""", recipeId, id, LocalDateTime.now(clock));
		jdbcTemplate.update("""
				INSERT INTO notifications (user_id, type, ingredient_id, message, created_at)
				VALUES (?, 'EXPIRY_SOON', ?, '소비기한이 임박했어요', ?)
				""", other, id, LocalDateTime.now(clock));

		deleteUseCase.deleteIngredientsForUser(me);

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM recipe_ingredients WHERE recipe_id = ?",
				Long.class, recipeId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT ingredient_id FROM recipe_ingredients WHERE recipe_id = ?",
				Long.class, recipeId)).isNull();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM notifications WHERE user_id = ?", Long.class,
				other)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT ingredient_id FROM notifications WHERE user_id = ?",
				Long.class, other)).isNull();
	}
}
