package com.remake.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.cart.application.port.in.AddCartItemsUseCase;
import com.remake.cart.application.port.in.CartAdditionCommand;
import com.remake.cart.application.port.in.UpdateCartItemUseCase;
import com.remake.cart.domain.CartSource;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.recipe.domain.RecipeUnit;
import com.remake.support.IntegrationTestSupport;

class CartAdditionTest extends IntegrationTestSupport {
	@Autowired private AddCartItemsUseCase add;
	@Autowired private UpdateCartItemUseCase update;

	private CartAdditionCommand command(Long recipe, String name, String amount, RecipeUnit unit) {
		return new CartAdditionCommand(recipe, null, name, amount == null ? null : new BigDecimal(amount), unit,
				IngredientCategoryCode.ETC);
	}

	private CartAdditionCommand atSlot(Long recipe, Long slot, String name, String amount) {
		return new CartAdditionCommand(recipe, slot, name, new BigDecimal(amount), RecipeUnit.EA, IngredientCategoryCode.VEGETABLE);
	}

	private BigDecimal amount(Long recipe, String name) {
		return jdbcTemplate.queryForObject("SELECT amount FROM cart_items WHERE recipe_id = ? AND name = ?", BigDecimal.class, recipe, name);
	}

	private Long id(Long recipe, String name) {
		return jdbcTemplate.queryForObject("SELECT id FROM cart_items WHERE recipe_id = ? AND name = ?", Long.class, recipe, name);
	}

	private Long slot(Long user, Long recipe) {
		LocalDateTime now = LocalDateTime.now(clock);
		Long plan = jdbcTemplate.queryForObject("""
				INSERT INTO meal_plans (user_id, start_date, meal_types, servings, status, created_at, updated_at)
				VALUES (?, ?, 'DINNER', 1, 'COMPLETED', ?, ?) RETURNING id
				""", Long.class, user, LocalDate.now(clock), now, now);
		return jdbcTemplate.queryForObject("""
				INSERT INTO meal_plan_slots(meal_plan_id, slot_date, meal_type, recipe_id, created_at, updated_at)
				VALUES (?, ?, 'DINNER', ?, ?, ?) RETURNING id
				""", Long.class, plan, LocalDate.now(clock), recipe, now, now);
	}

	@Test
	void 세출처의_이벤트는_명세값으로_한번씩_실제추가행수를_기록한다() {
		Long user = createUser();
		for (CartSource source : CartSource.values()) {
			Long recipe = createRecipe(user);
			assertThat(add.add(user, source, List.of(command(recipe, "양파", "2", RecipeUnit.EA),
					command(recipe, "당근", "3", RecipeUnit.EA)))).isEqualTo(2);
		}
		awaitEventWrites();
		var events = jdbcTemplate.queryForList("SELECT properties->>'cart_source' AS source, properties->>'item_count' AS count, properties::text AS props FROM events WHERE event_name='cart_item_added'");
		assertThat(events).hasSize(3);
		assertThat(events.stream().map(e -> e.get("source"))).containsExactlyInAnyOrder("own_recipe", "shared_recipe", "meal_plan");
		events.forEach(e -> {
			assertThat(e.get("count")).isEqualTo("2");
			assertThat(e.get("props").toString()).doesNotContain("양파", "당근", "ingredientName", "email");
		});
	}

	@Test
	void 빈목록과_양이없는재료와_물은_추가하지않고_이벤트도_없다() {
		Long user = createUser(); Long recipe = createRecipe(user);
		assertThat(add.add(user, CartSource.OWN_RECIPE, List.of())).isZero();
		assertThat(add.add(user, CartSource.OWN_RECIPE, List.of(
				command(recipe, "양파", null, RecipeUnit.EA), command(recipe, "소금", "1", RecipeUnit.PINCH),
				command(recipe, "후추", null, RecipeUnit.TO_TASTE), command(recipe, "물", "200", RecipeUnit.ML)))).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM cart_items", Long.class)).isZero();
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM events", Long.class)).isZero();
	}

	@Test
	void 같은레시피의_기존행과_수정수량은_유지하고_없는재료만_추가한다() {
		Long user = createUser(); Long recipe = createRecipe(user);
		assertThat(add.add(user, CartSource.OWN_RECIPE, List.of(command(recipe, "양파", "2", RecipeUnit.EA)))).isEqualTo(1);
		update.updateAmount(user, id(recipe, "양파"), new BigDecimal("7"));
		assertThat(add.add(user, CartSource.SHARED_RECIPE, List.of(command(recipe, "양파", "9", RecipeUnit.EA),
				command(recipe, "당근", "3", RecipeUnit.EA)))).isEqualTo(1);
		assertThat(amount(recipe, "양파")).isEqualByComparingTo("7");
		assertThat(amount(recipe, "당근")).isEqualByComparingTo("3");
		assertThat(add.add(user, CartSource.OWN_RECIPE, List.of(command(recipe, "양파", "1", RecipeUnit.EA)))).isZero();
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM events WHERE event_name='cart_item_added'", Long.class)).isEqualTo(2);
	}

	@Test
	void 기존과_다른레시피_사용자_단위_이름은_별도행이다() {
		Long user = createUser(); Long other = createUser(); Long recipe = createRecipe(user); Long second = createRecipe(user);
		assertThat(add.add(user, CartSource.OWN_RECIPE, List.of(command(recipe, "양파", "1", RecipeUnit.EA),
				command(recipe, "양파", "200", RecipeUnit.G), command(recipe, "유기농 양파", "2", RecipeUnit.EA),
				command(second, "양파", "3", RecipeUnit.EA)))).isEqualTo(4);
		assertThat(add.add(other, CartSource.SHARED_RECIPE, List.of(command(recipe, "양파", "4", RecipeUnit.EA)))).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM cart_items", Long.class)).isEqualTo(5);
	}

	@Test
	void 사전의_카테고리가_입력값보다_우선이고_앞뒤공백은_정리한다() {
		Long user = createUser(); Long recipe = createRecipe(user);
		assertThat(add.add(user, CartSource.OWN_RECIPE,
				List.of(command(recipe, " 양파 ", "1", RecipeUnit.EA), command(recipe, "양파", "3", RecipeUnit.EA)))).isEqualTo(1);
		Long catalogCategory = jdbcTemplate.queryForObject("SELECT category_id FROM ingredient_catalog WHERE name='양파'", Long.class);
		assertThat(jdbcTemplate.queryForObject("SELECT category_id FROM cart_items", Long.class)).isEqualTo(catalogCategory);
		assertThat(amount(recipe, "양파")).isEqualByComparingTo("1");
	}

	@Test
	void 사전에없는_이름은_서버확정_카테고리를_사용하고_둘다없으면_전체거절한다() {
		Long user = createUser(); Long recipe = createRecipe(user);
		assertThat(add.add(user, CartSource.OWN_RECIPE, List.of(command(recipe, "사전에없는재료", "2", RecipeUnit.G)))).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT category_id FROM cart_items", Long.class)).isEqualTo(categoryId("ETC"));
		assertThatThrownBy(() -> add.add(user, CartSource.OWN_RECIPE, List.of(command(recipe, "당근", "1", RecipeUnit.EA),
				new CartAdditionCommand(recipe, null, "또다른미등록재료", BigDecimal.ONE, RecipeUnit.EA, null))))
				.isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM cart_items", Long.class)).isEqualTo(1);
	}

	@Test
	void 이번식단의_같은원본_부족분을_먼저합산하고_최초슬롯출처를_남긴다() {
		Long user = createUser(); Long recipe = createRecipe(user); Long firstSlot = slot(user, recipe); Long secondSlot = slot(user, recipe);
		assertThat(add.add(user, CartSource.MEAL_PLAN, List.of(atSlot(recipe, firstSlot, "양파", "1"),
				atSlot(recipe, secondSlot, "양파", "2"), atSlot(recipe, firstSlot, "당근", "2"),
				atSlot(recipe, secondSlot, "당근", "2")))).isEqualTo(2);
		assertThat(amount(recipe, "양파")).isEqualByComparingTo("3");
		assertThat(amount(recipe, "당근")).isEqualByComparingTo("4");
		assertThat(jdbcTemplate.queryForObject("SELECT meal_plan_slot_id FROM cart_items WHERE name='양파'", Long.class)).isEqualTo(firstSlot);
	}

	@Test
	void 식단합산분을_기존수정량에_더하거나_최초슬롯을_바꾸지않는다() {
		Long user = createUser(); Long recipe = createRecipe(user); Long first = slot(user, recipe); Long next = slot(user, recipe);
		add.add(user, CartSource.MEAL_PLAN, List.of(atSlot(recipe, first, "양파", "1")));
		update.updateAmount(user, id(recipe, "양파"), new BigDecimal("5"));
		assertThat(add.add(user, CartSource.MEAL_PLAN, List.of(atSlot(recipe, next, "양파", "2"),
				atSlot(recipe, next, "당근", "2"), atSlot(recipe, first, "당근", "2")))).isEqualTo(1);
		assertThat(amount(recipe, "양파")).isEqualByComparingTo("5");
		assertThat(amount(recipe, "당근")).isEqualByComparingTo("4");
		assertThat(jdbcTemplate.queryForObject("SELECT meal_plan_slot_id FROM cart_items WHERE name='양파'", Long.class)).isEqualTo(first);
		jdbcTemplate.update("DELETE FROM meal_plan_slots WHERE id = ?", first);
		assertThat(amount(recipe, "양파")).isEqualByComparingTo("5");
		assertThat(jdbcTemplate.queryForObject("SELECT meal_plan_slot_id FROM cart_items WHERE name='양파'", Long.class)).isNull();
	}

	@Test
	void 이번식단의_합산오버플로는_전체거절한다() {
		Long user = createUser(); Long recipe = createRecipe(user);
		assertThatThrownBy(() -> add.add(user, CartSource.MEAL_PLAN, List.of(command(recipe, "양파", "6000000", RecipeUnit.EA),
				command(recipe, "양파", "6000000", RecipeUnit.EA))))
				.isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM cart_items", Long.class)).isZero();
	}

	@Test
	void 잘못된명령은_400이고_레시피출처와_식단슬롯_입력을_구분한다() {
		Long user = createUser(); Long recipe = createRecipe(user);
		for (var command : Arrays.asList(null,
				new CartAdditionCommand(null, null, "양파", BigDecimal.ONE, RecipeUnit.EA, null),
				new CartAdditionCommand(recipe, null, " ", BigDecimal.ONE, RecipeUnit.EA, null),
				new CartAdditionCommand(recipe, null, "양파", BigDecimal.ONE, null, null),
				new CartAdditionCommand(recipe, 1L, "양파", BigDecimal.ONE, RecipeUnit.EA, null),
				command(recipe, "양파", "0", RecipeUnit.EA), command(recipe, "양파", "-1", RecipeUnit.EA),
				command(recipe, "양파", "0.0001", RecipeUnit.EA), command(recipe, "양파", "10000000", RecipeUnit.EA))) {
			assertThatThrownBy(() -> add.add(user, CartSource.OWN_RECIPE, Arrays.asList(command)))
					.isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
		}
		assertThatThrownBy(() -> add.add(null, CartSource.OWN_RECIPE, List.of())).isInstanceOf(BusinessException.class);
		assertThatThrownBy(() -> add.add(user, null, List.of())).isInstanceOf(BusinessException.class);
		assertThatThrownBy(() -> add.add(user, CartSource.OWN_RECIPE, null)).isInstanceOf(BusinessException.class);
	}

	@Test
	void 같은키를_동시에담아도_한행과_이벤트한건만_남는다() throws Exception {
		Long user = createUser(); Long recipe = createRecipe(user); CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			Callable<Integer> action = () -> { start.await(); return add.add(user, CartSource.OWN_RECIPE,
					List.of(command(recipe, "양파", "2", RecipeUnit.EA))); };
			var first = executor.submit(action); var second = executor.submit(action); start.countDown();
			assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(1, 0);
		}
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM cart_items", Long.class)).isEqualTo(1);
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM events WHERE event_name='cart_item_added'", Long.class)).isEqualTo(1);
	}

	@Test
	void 반대순서의_여러재료_동시담기도_교착없이_한번만_추가한다() throws Exception {
		Long user = createUser(); Long recipe = createRecipe(user); CountDownLatch start = new CountDownLatch(1);
		var onion = command(recipe, "양파", "2", RecipeUnit.EA); var carrot = command(recipe, "당근", "3", RecipeUnit.EA);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var first = executor.submit(() -> { start.await(); return add.add(user, CartSource.OWN_RECIPE, List.of(onion, carrot)); });
			var second = executor.submit(() -> { start.await(); return add.add(user, CartSource.OWN_RECIPE, List.of(carrot, onion)); });
			start.countDown();
			assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(2, 0);
		}
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM cart_items", Long.class)).isEqualTo(2);
	}

	@Test
	void 호출자_실패면_추가행과_성공이벤트가_남지않는다() {
		Long user = createUser(); Long recipe = createRecipe(user);
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(tx -> {
			add.add(user, CartSource.OWN_RECIPE, List.of(command(recipe, "양파", "2", RecipeUnit.EA)));
			throw new IllegalStateException("호출자 실패");
		})).isInstanceOf(IllegalStateException.class);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM cart_items", Long.class)).isZero();
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM events", Long.class)).isZero();
	}

	@Test
	void 추가도중_DB실패면_앞서_추가한행도_롤백된다() {
		Long user = createUser(); Long recipe = createRecipe(user);
		jdbcTemplate.execute("ALTER TABLE cart_items ADD CONSTRAINT test_reject_add CHECK (name <> '양파')");
		try {
			assertThatThrownBy(() -> add.add(user, CartSource.OWN_RECIPE,
					List.of(command(recipe, "당근", "1", RecipeUnit.EA), command(recipe, "양파", "2", RecipeUnit.EA))))
					.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
			assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM cart_items", Long.class)).isZero();
			awaitEventWrites();
			assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM events", Long.class)).isZero();
		} finally { jdbcTemplate.execute("ALTER TABLE cart_items DROP CONSTRAINT test_reject_add"); }
	}
}
