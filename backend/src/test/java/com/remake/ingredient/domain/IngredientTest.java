package com.remake.ingredient.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

// 도메인 규칙: Spring 없이 확인한다.
class IngredientTest {

	private static final LocalDate EXPIRATION = LocalDate.of(2026, 3, 11);

	private static Ingredient register(String name, BigDecimal amount, Long price) {
		return Ingredient.register(1L, 2L, null, name, StorageLocation.FRIDGE, EXPIRATION, amount, IngredientUnit.EA,
				price);
	}

	private static Ingredient register() {
		return register("우유", new BigDecimal("2"), 3000L);
	}

	@Test
	void 등록하면_남은_양과_처음_양이_같다() {
		Ingredient ingredient = register("우유", new BigDecimal("2.5"), 3000L);

		assertThat(ingredient.amount()).isEqualByComparingTo("2.5");
		assertThat(ingredient.initialAmount()).isEqualByComparingTo("2.5");
	}

	@Test
	void 이름_앞뒤_공백은_지운다() {
		assertThat(register("  우유  ", BigDecimal.ONE, null).name()).isEqualTo("우유");
	}

	@Test
	void 이름은_비울_수_없다() {
		assertThatThrownBy(() -> register("   ", BigDecimal.ONE, null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> register(null, BigDecimal.ONE, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 이름은_50자까지이다() {
		assertThat(register("가".repeat(50), BigDecimal.ONE, null).name()).hasSize(50);
		assertThatThrownBy(() -> register("가".repeat(51), BigDecimal.ONE, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 수량은_0보다_커야_한다() {
		assertThatThrownBy(() -> register("우유", BigDecimal.ZERO, null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> register("우유", new BigDecimal("-1"), null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> register("우유", null, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 수량은_소수_셋째_자리까지이다() {
		assertThat(register("우유", new BigDecimal("0.001"), null).amount()).isEqualByComparingTo("0.001");
		// 뒤의 0은 자릿수로 세지 않는다
		assertThat(register("우유", new BigDecimal("1.500000"), null).amount()).isEqualByComparingTo("1.5");
		assertThatThrownBy(() -> register("우유", new BigDecimal("0.0001"), null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 수량은_정수_7자리까지이다() {
		assertThat(register("쌀", new BigDecimal("9999999.999"), null).amount())
				.isEqualByComparingTo("9999999.999");
		assertThatThrownBy(() -> register("쌀", new BigDecimal("10000000"), null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 가격은_없거나_0원_이상이다() {
		assertThat(register("우유", BigDecimal.ONE, null).price()).isNull();
		assertThat(register("우유", BigDecimal.ONE, 0L).price()).isZero();
		assertThatThrownBy(() -> register("우유", BigDecimal.ONE, -1L))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 필수값이_없으면_등록할_수_없다() {
		assertThatThrownBy(() -> Ingredient.register(null, 2L, null, "우유", StorageLocation.FRIDGE, EXPIRATION,
				BigDecimal.ONE, IngredientUnit.EA, null)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Ingredient.register(1L, 2L, null, "우유", null, EXPIRATION, BigDecimal.ONE,
				IngredientUnit.EA, null)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Ingredient.register(1L, 2L, null, "우유", StorageLocation.FRIDGE, null,
				BigDecimal.ONE, IngredientUnit.EA, null)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Ingredient.register(1L, 2L, null, "우유", StorageLocation.FRIDGE, EXPIRATION,
				BigDecimal.ONE, null, null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 소비기한을_고친다() {
		Ingredient ingredient = register();

		ingredient.changeExpirationDate(LocalDate.of(2026, 3, 20));

		assertThat(ingredient.expirationDate()).isEqualTo(LocalDate.of(2026, 3, 20));
		assertThatThrownBy(() -> ingredient.changeExpirationDate(null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 단위를_바꿔도_수량_숫자는_그대로다() {
		Ingredient ingredient = register("대파", new BigDecimal("2"), null);

		ingredient.changeUnit(IngredientUnit.BUNCH);

		assertThat(ingredient.unit()).isEqualTo(IngredientUnit.BUNCH);
		assertThat(ingredient.amount()).isEqualByComparingTo("2");
		assertThat(ingredient.initialAmount()).isEqualByComparingTo("2");
		assertThatThrownBy(() -> ingredient.changeUnit(null)).isInstanceOf(IllegalArgumentException.class);
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
		Ingredient ingredient = register("우유", new BigDecimal("2"), null);

		ingredient.changeAmount(new BigDecimal("3"));

		assertThat(ingredient.amount()).isEqualByComparingTo("3");
		assertThat(ingredient.initialAmount()).isEqualByComparingTo("3");
	}

	@Test
	void 수량_규칙을_어기면_고칠_수_없다() {
		Ingredient ingredient = register();

		assertThatThrownBy(() -> ingredient.changeAmount(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ingredient.changeAmount(new BigDecimal("1.2345"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ingredient.changeAmount(null)).isInstanceOf(IllegalArgumentException.class);
	}

	// ---- 사용·폐기(consume) ----

	private static Ingredient pack(String amount, Long price) {
		return Ingredient.register(1L, 1L, null, "우유", StorageLocation.FRIDGE, LocalDate.of(2026, 3, 10),
				new BigDecimal(amount), IngredientUnit.PACK, price);
	}

	@Test
	void 일부를_빼면_남은_양만_줄고_비지_않는다() {
		Ingredient ingredient = pack("4", 4000L);

		ingredient.consume(new BigDecimal("1.5"));

		assertThat(ingredient.amount()).isEqualByComparingTo("2.5");
		assertThat(ingredient.initialAmount()).isEqualByComparingTo("4");
		assertThat(ingredient.isEmpty()).isFalse();
	}

	@Test
	void 남은_양을_모두_빼면_0이_되고_비었다고_본다() {
		Ingredient ingredient = pack("2", null);

		ingredient.consume(new BigDecimal("2"));

		assertThat(ingredient.amount()).isEqualByComparingTo("0");
		assertThat(ingredient.isEmpty()).isTrue();
	}

	@Test
	void 남은_양보다_많이_빼면_InsufficientAmountException이다() {
		Ingredient ingredient = pack("2", null);

		assertThatThrownBy(() -> ingredient.consume(new BigDecimal("2.001")))
				.isInstanceOfSatisfying(InsufficientAmountException.class,
						e -> assertThat(e.remaining()).isEqualByComparingTo("2"));
		assertThat(ingredient.amount()).isEqualByComparingTo("2");
	}

	@Test
	void 수량이_0이하거나_형식이_틀리면_거절한다() {
		Ingredient ingredient = pack("2", null);

		assertThatThrownBy(() -> ingredient.consume(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ingredient.consume(new BigDecimal("-1"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ingredient.consume(new BigDecimal("0.0001"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ingredient.consume(null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 버린_금액은_가격에_처음_양_대비_비율을_곱해_원_단위로_반올림한다() {
		assertThat(pack("4", 4000L).proportionalCost(new BigDecimal("1"))).isEqualTo(1000L);
		assertThat(pack("3", 1000L).proportionalCost(new BigDecimal("1"))).isEqualTo(333L);
		assertThat(pack("3", 1000L).proportionalCost(new BigDecimal("2"))).isEqualTo(667L);
		assertThat(pack("4", 4000L).proportionalCost(new BigDecimal("4"))).isEqualTo(4000L);
		assertThat(pack("4", 0L).proportionalCost(new BigDecimal("1"))).isZero();
	}

	@Test
	void 가격이_없으면_버린_금액은_null이다() {
		assertThat(pack("4", null).proportionalCost(new BigDecimal("1"))).isNull();
	}

	@Test
	void 일부를_쓴_뒤에도_처음_양_기준으로_비율을_계산한다() {
		Ingredient ingredient = pack("4", 4000L);
		ingredient.consume(new BigDecimal("1"));

		assertThat(ingredient.proportionalCost(new BigDecimal("1"))).isEqualTo(1000L);
	}
}
