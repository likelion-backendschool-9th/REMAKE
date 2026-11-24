package com.remake.ingredient.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.ingredient.domain.Ingredient;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;
import com.remake.support.IntegrationTestSupport;

// 도메인 객체 ↔ 엔티티 변환과 DB 저장을 확인한다. 어댑터는 운영처럼 트랜잭션 안에서 부른다.
class IngredientPersistenceAdapterTest extends IntegrationTestSupport {

	@Autowired
	private IngredientPersistenceAdapter adapter;

	@Test
	void 새_재료를_저장하면_id가_채워지고_같은_값으로_읽힌다() {
		ensureUser(101L);
		Ingredient registered = Ingredient.register(101L, categoryId("MEAT"), 77L, "삼겹살", StorageLocation.FREEZER,
				LocalDate.of(2026, 3, 12), new BigDecimal("0.001"), IngredientUnit.KG, 12000L);

		Ingredient saved = transactionTemplate.execute(status -> adapter.save(registered));
		Ingredient loaded = transactionTemplate
				.execute(status -> adapter.loadByIdAndUserId(saved.id(), 101L).orElseThrow());

		assertThat(saved.id()).isNotNull();
		assertThat(loaded.userId()).isEqualTo(101L);
		assertThat(loaded.categoryId()).isEqualTo(categoryId("MEAT"));
		assertThat(loaded.receiptItemId()).isEqualTo(77L);
		assertThat(loaded.name()).isEqualTo("삼겹살");
		assertThat(loaded.storage()).isEqualTo(StorageLocation.FREEZER);
		assertThat(loaded.expirationDate()).isEqualTo(LocalDate.of(2026, 3, 12));
		assertThat(loaded.amount()).isEqualByComparingTo("0.001");
		assertThat(loaded.initialAmount()).isEqualByComparingTo("0.001");
		assertThat(loaded.unit()).isEqualTo(IngredientUnit.KG);
		assertThat(loaded.price()).isEqualTo(12000L);
	}

	@Test
	void 저장한_재료는_생성_시각이_시험용_시계를_따른다() {
		ensureUser(101L);
		Ingredient registered = Ingredient.register(101L, categoryId("VEGETABLE"), null, "양파",
				StorageLocation.FRIDGE, LocalDate.of(2026, 3, 12), BigDecimal.ONE, IngredientUnit.EA, null);

		Ingredient saved = transactionTemplate.execute(status -> adapter.save(registered));

		// 기본 시각 2026-03-04 10:00 (서울)
		assertThat(jdbcTemplate.queryForObject("SELECT created_at::date FROM ingredients WHERE id = ?",
				java.sql.Date.class, saved.id()).toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 4));
	}

	@Test
	void 다른_사용자_번호로는_읽히지_않는다() {
		ensureUser(101L);
		Ingredient registered = Ingredient.register(101L, categoryId("VEGETABLE"), null, "양파",
				StorageLocation.FRIDGE, LocalDate.of(2026, 3, 12), BigDecimal.ONE, IngredientUnit.EA, null);
		Ingredient saved = transactionTemplate.execute(status -> adapter.save(registered));

		Boolean visible = transactionTemplate
				.execute(status -> adapter.loadByIdAndUserId(saved.id(), 102L).isPresent());

		assertThat(visible).isFalse();
	}
}
