package com.remake.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.cart.application.port.in.AutomaticCartDeductionCommand;
import com.remake.cart.application.port.in.DeductCartItemsUseCase;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.support.IntegrationTestSupport;

class AutomaticCartDeductionTest extends IntegrationTestSupport {
	@Autowired private DeductCartItemsUseCase deduct;

	private Long item(Long user, String name, String amount, String unit) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO cart_items (user_id, recipe_id, category_id, name, amount, unit, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
				""", Long.class, user, createRecipe(user), categoryId("VEGETABLE"), name,
				new BigDecimal(amount), unit, now, now);
	}

	private AutomaticCartDeductionCommand command(String amount) {
		return new AutomaticCartDeductionCommand("양파", new BigDecimal(amount), IngredientUnit.EA);
	}

	private BigDecimal amount(Long id) {
		return jdbcTemplate.queryForObject("SELECT amount FROM cart_items WHERE id = ?", BigDecimal.class, id);
	}

	@Test
	void 이름단위별_합산량을_ID순으로_차감하고_삭제된행만_센다() {
		Long user = createUser();
		Long first = item(user, "양파", "2", "EA");
		Long second = item(user, "양파", "5", "EA");
		Long third = item(user, "양파", "8", "EA");
		clock.advance(java.time.Duration.ofMinutes(1));
		assertThat(deduct.deductAutomatically(user, List.of(command("1"), command("2")))).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM cart_items WHERE id = ?", Long.class, first)).isZero();
		assertThat(amount(second)).isEqualByComparingTo("4");
		assertThat(amount(third)).isEqualByComparingTo("8");
		var row = jdbcTemplate.queryForMap("SELECT created_at, updated_at FROM cart_items WHERE id = ?", second);
		assertThat((java.sql.Timestamp) row.get("updated_at")).isAfter((java.sql.Timestamp) row.get("created_at"));
	}

	@Test
	void 총잔량을_초과해도_잔량까지만_처리하고_전부삭제한다() {
		Long user = createUser();
		item(user, "양파", "2", "EA"); item(user, "양파", "3", "EA");
		assertThat(deduct.deductAutomatically(user, List.of(command("9")))).isEqualTo(2);
		assertThat(deduct.deductAutomatically(user, List.of(command("9")))).isZero();
	}

	@Test
	void 다른사용자_이름_공백_단위는_일치하지않고_환산하지않는다() {
		Long user = createUser(); Long other = createUser();
		Long foreign = item(other, "양파", "4", "EA");
		Long name = item(user, " 양파", "4", "EA");
		Long unit = item(user, "양파", "1000", "G");
		Long spoon = item(user, "양파", "1", "TBSP");
		assertThat(deduct.deductAutomatically(user, List.of(command("4")))).isZero();
		assertThat(amount(foreign)).isEqualByComparingTo("4");
		assertThat(amount(name)).isEqualByComparingTo("4");
		assertThat(amount(unit)).isEqualByComparingTo("1000");
		assertThat(amount(spoon)).isEqualByComparingTo("1");
	}

	@Test
	void 여러이름과_단위를_독립처리하고_소수셋째자리를_보존한다() {
		Long user = createUser();
		Long ea = item(user, "양파", "2", "EA");
		Long grams = item(user, "양파", "2", "G");
		Long carrot = item(user, "당근", "1", "EA");
		assertThat(deduct.deductAutomatically(user, List.of(command("0.001"),
				new AutomaticCartDeductionCommand("양파", BigDecimal.ONE, IngredientUnit.G),
				new AutomaticCartDeductionCommand("당근", BigDecimal.ONE, IngredientUnit.EA)))).isEqualTo(1);
		assertThat(amount(ea)).isEqualByComparingTo("1.999");
		assertThat(amount(grams)).isEqualByComparingTo("1");
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM cart_items WHERE id = ?", Long.class, carrot)).isZero();
	}

	@Test
	void 빈목록과_매칭없는입력은_0이다() {
		Long user = createUser();
		assertThat(deduct.deductAutomatically(user, List.of())).isZero();
		assertThat(deduct.deductAutomatically(user, List.of(command("1")))).isZero();
	}

	@Test
	void 잘못된입력은_변경전에_모두검증한다() {
		Long user = createUser(); Long id = item(user, "양파", "5", "EA");
		for (var invalid : Arrays.asList(null,
				new AutomaticCartDeductionCommand(null, BigDecimal.ONE, IngredientUnit.EA),
				new AutomaticCartDeductionCommand(" ", BigDecimal.ONE, IngredientUnit.EA),
				new AutomaticCartDeductionCommand("양파", BigDecimal.ONE, null),
				new AutomaticCartDeductionCommand("양파", null, IngredientUnit.EA),
				command("0"), command("-1"), command("0.0001"), command("10000000"))) {
			assertThatThrownBy(() -> deduct.deductAutomatically(user, Arrays.asList(command("1"), invalid)))
					.isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
		}
		for (Long invalid : Arrays.asList(null, 0L, -1L)) {
			assertThatThrownBy(() -> deduct.deductAutomatically(invalid, List.of()))
					.isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
		}
		assertThatThrownBy(() -> deduct.deductAutomatically(user, null)).isInstanceOf(BusinessException.class);
		assertThat(amount(id)).isEqualByComparingTo("5");
	}

	@Test
	void 개별수량이_유효하면_합산이_DB범위를넘어도_잔량까지만_차감한다() {
		Long user = createUser(); item(user, "양파", "1", "EA");
		assertThat(deduct.deductAutomatically(user, List.of(command("9999999"), command("9999999")))).isEqualTo(1);
	}

	@Test
	void 호출자실패시_삭제와_부분차감이_모두롤백된다() {
		Long user = createUser(); Long first = item(user, "양파", "2", "EA");
		Long second = item(user, "양파", "5", "EA");
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(tx -> {
			assertThat(deduct.deductAutomatically(user, List.of(command("3")))).isEqualTo(1);
			throw new IllegalStateException("등록 실패");
		})).isInstanceOf(IllegalStateException.class);
		assertThat(amount(first)).isEqualByComparingTo("2");
		assertThat(amount(second)).isEqualByComparingTo("5");
	}

	@Test
	void DB저장실패시_앞서삭제한행도_롤백된다() {
		Long user = createUser(); Long first = item(user, "양파", "2", "EA");
		Long second = item(user, "양파", "5", "EA");
		jdbcTemplate.execute("ALTER TABLE cart_items ADD CONSTRAINT test_auto_deduction CHECK (amount <> 4)");
		try {
			assertThatThrownBy(() -> deduct.deductAutomatically(user, List.of(command("3"))))
					.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
			assertThat(amount(first)).isEqualByComparingTo("2");
			assertThat(amount(second)).isEqualByComparingTo("5");
		} finally {
			jdbcTemplate.execute("ALTER TABLE cart_items DROP CONSTRAINT test_auto_deduction");
		}
	}

	@Test
	void 동시요청은_잠금해제후_잔량만처리하고_삭제를_중복집계하지않는다() throws Exception {
		Long user = createUser(); item(user, "양파", "2", "EA"); item(user, "양파", "3", "EA");
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			java.util.concurrent.Callable<Integer> action = () -> {
				start.await();
				return deduct.deductAutomatically(user, List.of(command("4")));
			};
			var first = executor.submit(action); var second = executor.submit(action); start.countDown();
			assertThat(first.get(15, TimeUnit.SECONDS) + second.get(15, TimeUnit.SECONDS)).isEqualTo(2);
		}
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM cart_items", Long.class)).isZero();
	}

	@Test
	void 호출자트랜잭션이_끝날때까지_다음차감은_행잠금을_기다린다() throws Exception {
		Long user = createUser(); item(user, "양파", "5", "EA");
		CountDownLatch locked = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch attempting = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var first = executor.submit(() -> transactionTemplate.executeWithoutResult(tx -> {
				assertThat(deduct.deductAutomatically(user, List.of(command("1")))).isZero();
				locked.countDown();
				try {
					assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException(e);
				}
			}));
			try {
				assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
				var second = executor.submit(() -> {
					attempting.countDown();
					return deduct.deductAutomatically(user, List.of(command("5")));
				});
				assertThat(attempting.await(10, TimeUnit.SECONDS)).isTrue();
				assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS))
						.isInstanceOf(java.util.concurrent.TimeoutException.class);
				release.countDown();
				first.get(10, TimeUnit.SECONDS);
				assertThat(second.get(10, TimeUnit.SECONDS)).isEqualTo(1);
			} finally {
				release.countDown();
			}
		}
	}

}
