package com.remake.ingredient.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

// 도메인 규칙: Spring 없이 확인한다.
class IngredientTest {

	private static final LocalDate EXPIRATION = LocalDate.of(2026, 3, 11);

	private static Ingredient register(String name, BigDecimal amount, Long price, boolean estimated) {
		return Ingredient.register(1L, 2L, null, name, StorageLocation.FRIDGE, EXPIRATION, estimated, amount,
				IngredientUnit.EA, price);
	}

	private static Ingredient register() {
		return register("우유", new BigDecimal("2"), 3000L, false);
	}

	@Test
	void 등록하면_남은_양과_처음_양이_같고_ACTIVE이다() {
		Ingredient ingredient = register("우유", new BigDecimal("2.5"), 3000L, false);

		assertThat(ingredient.amount()).isEqualByComparingTo("2.5");
		assertThat(ingredient.initialAmount()).isEqualByComparingTo("2.5");
		assertThat(ingredient.status()).isEqualTo(IngredientStatus.ACTIVE);
		assertThat(ingredient.expirationEstimated()).isFalse();
	}

	@Test
	void 이름_앞뒤_공백은_지운다() {
		assertThat(register("  우유  ", BigDecimal.ONE, null, false).name()).isEqualTo("우유");
	}

	@Test
	void 이름은_비울_수_없다() {
		assertThatThrownBy(() -> register("   ", BigDecimal.ONE, null, false))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> register(null, BigDecimal.ONE, null, false))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 이름은_50자까지이다() {
		assertThat(register("가".repeat(50), BigDecimal.ONE, null, false).name()).hasSize(50);
		assertThatThrownBy(() -> register("가".repeat(51), BigDecimal.ONE, null, false))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 수량은_0보다_커야_한다() {
		assertThatThrownBy(() -> register("우유", BigDecimal.ZERO, null, false))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> register("우유", new BigDecimal("-1"), null, false))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> register("우유", null, null, false))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 수량은_소수_셋째_자리까지이다() {
		assertThat(register("우유", new BigDecimal("0.001"), null, false).amount()).isEqualByComparingTo("0.001");
		// 뒤의 0은 자릿수로 세지 않는다
		assertThat(register("우유", new BigDecimal("1.500000"), null, false).amount()).isEqualByComparingTo("1.5");
		assertThatThrownBy(() -> register("우유", new BigDecimal("0.0001"), null, false))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 수량은_정수_7자리까지이다() {
		assertThat(register("쌀", new BigDecimal("9999999.999"), null, false).amount())
				.isEqualByComparingTo("9999999.999");
		assertThatThrownBy(() -> register("쌀", new BigDecimal("10000000"), null, false))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 가격은_없거나_0원_이상이다() {
		assertThat(register("우유", BigDecimal.ONE, null, false).price()).isNull();
		assertThat(register("우유", BigDecimal.ONE, 0L, false).price()).isZero();
		assertThatThrownBy(() -> register("우유", BigDecimal.ONE, -1L, false))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 필수값이_없으면_등록할_수_없다() {
		assertThatThrownBy(() -> Ingredient.register(null, 2L, null, "우유", StorageLocation.FRIDGE, EXPIRATION, false,
				BigDecimal.ONE, IngredientUnit.EA, null)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Ingredient.register(1L, 2L, null, "우유", null, EXPIRATION, false, BigDecimal.ONE,
				IngredientUnit.EA, null)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Ingredient.register(1L, 2L, null, "우유", StorageLocation.FRIDGE, null, false,
				BigDecimal.ONE, IngredientUnit.EA, null)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Ingredient.register(1L, 2L, null, "우유", StorageLocation.FRIDGE, EXPIRATION, false,
				BigDecimal.ONE, null, null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 소비기한을_직접_고치면_추정_표시가_꺼진다() {
		Ingredient ingredient = register("우유", BigDecimal.ONE, null, true);
		assertThat(ingredient.expirationEstimated()).isTrue();

		ingredient.changeExpirationDate(LocalDate.of(2026, 3, 20));

		assertThat(ingredient.expirationDate()).isEqualTo(LocalDate.of(2026, 3, 20));
		assertThat(ingredient.expirationEstimated()).isFalse();
	}

	@Test
	void 소비기한을_같은_날짜로_고쳐도_추정_표시가_꺼진다() {
		Ingredient ingredient = register("우유", BigDecimal.ONE, null, true);

		ingredient.changeExpirationDate(EXPIRATION);

		assertThat(ingredient.expirationEstimated()).isFalse();
	}

	@Test
	void 보관_위치와_가격을_고친다() {
		Ingredient ingredient = register();

		ingredient.changeStorage(StorageLocation.FREEZER);
		ingredient.changePrice(null);

		assertThat(ingredient.storage()).isEqualTo(StorageLocation.FREEZER);
		assertThat(ingredient.price()).isNull();
		assertThatThrownBy(() -> ingredient.changePrice(-5L)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ingredient.changeStorage(null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 사용한_적이_없으면_수량을_고치고_처음_양도_함께_바뀐다() {
		Ingredient ingredient = register("우유", new BigDecimal("2"), null, false);

		ingredient.changeAmount(new BigDecimal("3"));

		assertThat(ingredient.amount()).isEqualByComparingTo("3");
		assertThat(ingredient.initialAmount()).isEqualByComparingTo("3");
	}

	@Test
	void 일부_사용했거나_완료된_재료는_수량을_고칠_수_없다() {
		Ingredient used = Ingredient.restore(1L, 1L, 1L, null, "우유", StorageLocation.FRIDGE, EXPIRATION, false,
				new BigDecimal("1"), new BigDecimal("2"), IngredientUnit.EA, null, IngredientStatus.ACTIVE);
		Ingredient finished = Ingredient.restore(2L, 1L, 1L, null, "우유", StorageLocation.FRIDGE, EXPIRATION, false,
				BigDecimal.ZERO, new BigDecimal("2"), IngredientUnit.EA, null, IngredientStatus.FINISHED);

		assertThatThrownBy(() -> used.changeAmount(new BigDecimal("5"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> finished.changeAmount(new BigDecimal("5"))).isInstanceOf(IllegalArgumentException.class);
		assertThat(used.amount()).isEqualByComparingTo("1");
		assertThat(used.initialAmount()).isEqualByComparingTo("2");
	}

	@Test
	void 수량_규칙을_어기면_고칠_수_없다() {
		Ingredient ingredient = register();

		assertThatThrownBy(() -> ingredient.changeAmount(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ingredient.changeAmount(new BigDecimal("1.2345"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ingredient.changeAmount(null)).isInstanceOf(IllegalArgumentException.class);
	}
}
