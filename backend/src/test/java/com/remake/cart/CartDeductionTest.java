package com.remake.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
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

import com.remake.cart.application.port.in.CartDeductionCommand;
import com.remake.cart.application.port.in.DeductCartItemsUseCase;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.RegisterIngredientCommand;
import com.remake.ingredient.application.port.in.RegisterIngredientUseCase;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;
import com.remake.support.IntegrationTestSupport;

class CartDeductionTest extends IntegrationTestSupport {
	@Autowired private DeductCartItemsUseCase deduct;
	@Autowired private RegisterIngredientUseCase register;

	private Long item(Long user, Long recipe, String amount) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO cart_items (user_id, recipe_id, category_id, name, amount, unit, created_at, updated_at)
				VALUES (?, ?, ?, '양파', ?, 'EA', ?, ?) RETURNING id
				""", Long.class, user, recipe, categoryId("VEGETABLE"), new BigDecimal(amount), now, now);
	}

	private CartDeductionCommand command(Long id, String amount) {
		return new CartDeductionCommand(id, "양파", new BigDecimal(amount), IngredientUnit.EA);
	}

	private BigDecimal amount(Long id) {
		return jdbcTemplate.queryForObject("SELECT amount FROM cart_items WHERE id = ?", BigDecimal.class, id);
	}

	private void rejects(Long user, List<CartDeductionCommand> commands, ErrorCode code) {
		assertThatThrownBy(() -> deduct.deduct(user, commands)).isInstanceOfSatisfying(BusinessException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(code));
	}

	@Test
	void 미선택은_0이고_기존_장바구니를_유지한다() {
		Long user = createUser(); Long id = item(user, createRecipe(user), "5");
		assertThat(deduct.deduct(user, List.of())).isZero();
		assertThat(amount(id)).isEqualByComparingTo("5");
	}

	@Test
	void 부분차감은_삭제수_0이고_수정시각을_갱신한다() {
		Long user = createUser(); Long id = item(user, createRecipe(user), "5");
		clock.advance(Duration.ofMinutes(1));
		assertThat(deduct.deduct(user, List.of(command(id, "2")))).isZero();
		assertThat(amount(id)).isEqualByComparingTo("3");
		var row = jdbcTemplate.queryForMap("SELECT created_at, updated_at FROM cart_items WHERE id = ?", id);
		assertThat(((java.sql.Timestamp) row.get("updated_at")).toLocalDateTime())
				.isAfter(((java.sql.Timestamp) row.get("created_at")).toLocalDateTime());
	}

	@Test
	void 같은대상을_여러품목이_선택하면_합산하고_삭제수는_한행이다() {
		Long user = createUser(); Long id = item(user, createRecipe(user), "3");
		assertThat(deduct.deduct(user, List.of(command(id, "1"), command(id, "2")))).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM cart_items", Long.class)).isZero();
	}

	@Test
	void 여러레시피의_같은이름도_선택한_항목만_차감하고_삭제된_행만_센다() {
		Long user = createUser(); Long first = item(user, createRecipe(user), "2");
		Long second = item(user, createRecipe(user), "5"); Long untouched = item(user, createRecipe(user), "8");
		assertThat(deduct.deduct(user, List.of(command(first, "2"), command(second, "1")))).isEqualTo(1);
		assertThat(amount(second)).isEqualByComparingTo("4");
		assertThat(amount(untouched)).isEqualByComparingTo("8");
	}

	@Test
	void 합산차감량이_잔량을_넘으면_모두_유지하고_잔량을_알려준다() {
		Long user = createUser(); Long first = item(user, createRecipe(user), "2");
		Long second = item(user, createRecipe(user), "3");
		assertThatThrownBy(() -> deduct.deduct(user,
				List.of(command(first, "2"), command(second, "2"), command(second, "2"))))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_QUANTITY);
					assertThat((BigDecimal) e.getDetails().get("remaining")).isEqualByComparingTo("3");
				});
		assertThat(amount(first)).isEqualByComparingTo("2");
		assertThat(amount(second)).isEqualByComparingTo("3");
	}

	@Test
	void 타인과_없는항목은_같은404이며_앞의_정상항목도_유지한다() {
		Long user = createUser(); Long other = createUser(); Long first = item(user, createRecipe(user), "2");
		Long foreign = item(other, createRecipe(other), "3");
		for (Long target : List.of(foreign, Long.MAX_VALUE)) {
			rejects(user, List.of(command(first, "1"), command(target, "1")), ErrorCode.CART_ITEM_NOT_FOUND);
		}
		assertThat(amount(first)).isEqualByComparingTo("2");
		assertThat(amount(foreign)).isEqualByComparingTo("3");
	}

	@Test
	void 이름과_단위가_다르면_환산하거나_이름을_추정하지_않는다() {
		Long user = createUser(); Long id = item(user, createRecipe(user), "5");
		for (var command : List.of(
				new CartDeductionCommand(id, " 양파", BigDecimal.ONE, IngredientUnit.EA),
				new CartDeductionCommand(id, "당근", BigDecimal.ONE, IngredientUnit.EA),
				new CartDeductionCommand(id, "양파", BigDecimal.ONE, IngredientUnit.KG))) {
			rejects(user, List.of(command), ErrorCode.INVALID_INPUT);
		}
		// 같은 id에 대한 첫 요청만 맞아도 뒤의 이름·단위 오류를 놓치지 않는다.
		rejects(user, List.of(command(id, "1"),
				new CartDeductionCommand(id, "당근", BigDecimal.ONE, IngredientUnit.EA)), ErrorCode.INVALID_INPUT);
		assertThat(amount(id)).isEqualByComparingTo("5");
	}

	@Test
	void 잘못된_수량과_필수값은_400이고_소수셋째자리는_허용한다() {
		Long user = createUser(); Long id = item(user, createRecipe(user), "5");
		for (BigDecimal value : Arrays.asList(null, BigDecimal.ZERO, new BigDecimal("-1"),
				new BigDecimal("0.0001"), new BigDecimal("10000000"))) {
			rejects(user, List.of(new CartDeductionCommand(id, "양파", value, IngredientUnit.EA)), ErrorCode.INVALID_INPUT);
		}
		for (var command : Arrays.asList(null,
				new CartDeductionCommand(null, "양파", BigDecimal.ONE, IngredientUnit.EA),
				new CartDeductionCommand(0L, "양파", BigDecimal.ONE, IngredientUnit.EA),
				new CartDeductionCommand(id, null, BigDecimal.ONE, IngredientUnit.EA),
				new CartDeductionCommand(id, "", BigDecimal.ONE, IngredientUnit.EA),
				new CartDeductionCommand(id, "양파", BigDecimal.ONE, null))) {
			rejects(user, Arrays.asList(command), ErrorCode.INVALID_INPUT);
		}
		rejects(user, null, ErrorCode.INVALID_INPUT);
		rejects(null, List.of(), ErrorCode.INVALID_INPUT);
		assertThat(deduct.deduct(user, List.of(command(id, "0.001")))).isZero();
		assertThat(amount(id)).isEqualByComparingTo("4.999");
	}

	@Test
	void 호출자의_냉장고등록과_차감이_함께_롤백된다() {
		Long user = createUser(); Long id = item(user, createRecipe(user), "5");
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(tx -> {
			register.registerIngredient(new RegisterIngredientCommand(user, "양파", IngredientCategoryCode.VEGETABLE,
					StorageLocation.FRIDGE, null, null, BigDecimal.ONE, IngredientUnit.EA, null, null));
			deduct.deduct(user, List.of(command(id, "6")));
		})).isInstanceOf(BusinessException.class);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredients", Long.class)).isZero();
		assertThat(amount(id)).isEqualByComparingTo("5");
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM events", Long.class)).isZero();
		// 차감 성공 뒤 호출자가 실패해도 장바구니 삭제가 되돌아온다.
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(tx -> {
			assertThat(deduct.deduct(user, List.of(command(id, "5")))).isEqualTo(1);
			throw new IllegalStateException("호출자 저장 실패");
		})).isInstanceOf(IllegalStateException.class);
		assertThat(amount(id)).isEqualByComparingTo("5");
	}

	@Test
	void 동시차감은_최신잔량으로_검증하고_음수가_되지않는다() throws Exception {
		Long user = createUser(); Long id = item(user, createRecipe(user), "5");
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			Callable<String> action = () -> {
				start.await();
				try { deduct.deduct(user, List.of(command(id, "3"))); return "OK"; }
				catch (BusinessException e) { return e.getErrorCode().name(); }
			};
			var first = executor.submit(action); var second = executor.submit(action); start.countDown();
			assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
					.containsExactlyInAnyOrder("OK", "INSUFFICIENT_QUANTITY");
		}
		assertThat(amount(id)).isEqualByComparingTo("2");
	}

	@Test
	void 같은행을_동시에_전부차감하면_후행은_없는항목이다() throws Exception {
		Long user = createUser(); Long id = item(user, createRecipe(user), "1");
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			Callable<String> action = () -> {
				start.await();
				try { return "removed=" + deduct.deduct(user, List.of(command(id, "1"))); }
				catch (BusinessException e) { return e.getErrorCode().name(); }
			};
			var first = executor.submit(action); var second = executor.submit(action); start.countDown();
			assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
					.containsExactlyInAnyOrder("removed=1", "CART_ITEM_NOT_FOUND");
		}
	}

	@Test
	void 차감후_DB저장실패면_앞의_행과_뒤의_행이_모두_롤백된다() {
		Long user = createUser(); Long first = item(user, createRecipe(user), "2");
		Long second = item(user, createRecipe(user), "5");
		jdbcTemplate.execute("ALTER TABLE cart_items ADD CONSTRAINT test_reject_deduction CHECK (amount <> 4)");
		try {
			assertThatThrownBy(() -> deduct.deduct(user, List.of(command(first, "2"), command(second, "1"))))
					.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
			assertThat(amount(first)).isEqualByComparingTo("2");
			assertThat(amount(second)).isEqualByComparingTo("5");
		} finally {
			jdbcTemplate.execute("ALTER TABLE cart_items DROP CONSTRAINT test_reject_deduction");
		}
	}

	@Test
	void 식단배정을_삭제해도_장바구니와_레시피는_유지한다() {
		Long user = createUser(); Long recipe = createRecipe(user); Long id = item(user, recipe, "5");
		LocalDateTime now = LocalDateTime.now(clock);
		Long plan = jdbcTemplate.queryForObject("""
				INSERT INTO meal_plans (user_id, start_date, meal_types, servings, created_at, updated_at)
				VALUES (?, ?, 'DINNER', 1, ?, ?) RETURNING id
				""", Long.class, user, LocalDate.now(clock), now, now);
		Long slot = jdbcTemplate.queryForObject("""
				INSERT INTO meal_plan_slots (meal_plan_id, slot_date, meal_type, recipe_id, created_at, updated_at)
				VALUES (?, ?, 'DINNER', ?, ?, ?) RETURNING id
				""", Long.class, plan, LocalDate.now(clock), recipe, now, now);
		jdbcTemplate.update("UPDATE cart_items SET meal_plan_slot_id = ? WHERE id = ?", slot, id);
		jdbcTemplate.update("DELETE FROM meal_plan_slots WHERE id = ?", slot);
		assertThat(jdbcTemplate.queryForObject("SELECT meal_plan_slot_id FROM cart_items WHERE id = ?", Long.class, id)).isNull();
		assertThat(jdbcTemplate.queryForObject("SELECT recipe_id FROM cart_items WHERE id = ?", Long.class, id)).isEqualTo(recipe);
		assertThat(amount(id)).isEqualByComparingTo("5");
	}
}
