package com.remake.ingredient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.GetIngredientUseCase;
import com.remake.ingredient.application.port.in.RegisterIngredientCommand;
import com.remake.ingredient.application.port.in.RegisterIngredientUseCase;
import com.remake.ingredient.domain.Ingredient;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;
import com.remake.support.IntegrationTestSupport;

// 재료 등록을 DB까지 확인한다. 기본 시각은 2026-03-04 10:00(서울)이라 오늘은 3월 4일이다.
// 사용자 ID(101)는 users 테이블이 없어 임의 번호다. users 마이그레이션이 외래 키를 추가하면 createUser()로 바꾼다.
class IngredientRegistrationTest extends IntegrationTestSupport {

	private static final Long USER = 101L;

	@Autowired
	private RegisterIngredientUseCase registerUseCase;

	@Autowired
	private GetIngredientUseCase getUseCase;

	private RegisterIngredientCommand command(String name, IngredientCategoryCode category, LocalDate expirationDate,
			LocalDate baseDate, boolean estimated, BigDecimal amount, Long receiptItemId) {
		return new RegisterIngredientCommand(USER, name, category, StorageLocation.FRIDGE, expirationDate, baseDate,
				estimated, amount, IngredientUnit.EA, 3000L, receiptItemId);
	}

	private void assertErrorCode(Runnable call, ErrorCode expected) {
		assertThatThrownBy(call::run)
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(expected);
	}

	private long countIngredients() {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM ingredients", Long.class);
	}

	@Test
	void 직접_추가하면_입력한_값이_그대로_저장되고_추정이_아니다() {
		Long id = registerUseCase.registerIngredient(command("우유", IngredientCategoryCode.DAIRY_EGG,
				LocalDate.of(2026, 3, 12), null, false, new BigDecimal("2"), null));

		Ingredient saved = getUseCase.getIngredient(USER, id);
		assertThat(saved.name()).isEqualTo("우유");
		assertThat(saved.categoryId()).isEqualTo(categoryId("DAIRY_EGG"));
		assertThat(saved.expirationDate()).isEqualTo(LocalDate.of(2026, 3, 12));
		assertThat(saved.expirationEstimated()).isFalse();
		assertThat(saved.amount()).isEqualByComparingTo("2");
		assertThat(saved.price()).isEqualTo(3000L);
		assertThat(saved.receiptItemId()).isNull();
	}

	@Test
	void 소비기한이_없으면_구매일에_기본_보관_일수를_더해_추정하고_추정_표시를_켠다() {
		// 채소의 기본 보관 일수는 7일이다.
		Long id = registerUseCase.registerIngredient(command("양파", IngredientCategoryCode.VEGETABLE, null,
				LocalDate.of(2026, 3, 1), false, BigDecimal.ONE, null));

		Ingredient saved = getUseCase.getIngredient(USER, id);
		assertThat(saved.expirationDate()).isEqualTo(LocalDate.of(2026, 3, 8));
		// 넘겨 준 추정 표시가 false여도 날짜를 재료 쪽에서 추정했으므로 true로 기록한다.
		assertThat(saved.expirationEstimated()).isTrue();
	}

	@Test
	void 기준일이_없으면_오늘을_기준으로_추정한다() {
		Long id = registerUseCase.registerIngredient(command("양파", IngredientCategoryCode.VEGETABLE, null, null,
				false, BigDecimal.ONE, null));

		assertThat(getUseCase.getIngredient(USER, id).expirationDate()).isEqualTo(LocalDate.of(2026, 3, 11));
	}

	@Test
	void OCR이_추정한_날짜를_그대로_확정하면_추정_표시를_유지한다() {
		Long id = registerUseCase.registerIngredient(command("양파", IngredientCategoryCode.VEGETABLE,
				LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 1), true, BigDecimal.ONE, 55L));

		Ingredient saved = getUseCase.getIngredient(USER, id);
		assertThat(saved.expirationDate()).isEqualTo(LocalDate.of(2026, 3, 9));
		assertThat(saved.expirationEstimated()).isTrue();
	}

	@Test
	void 영수증은_여러_건을_입력_순서대로_등록하고_품목을_연결한다() {
		List<Long> ids = registerUseCase.registerIngredients(List.of(
				command("우유", IngredientCategoryCode.DAIRY_EGG, null, LocalDate.of(2026, 3, 2), false, BigDecimal.ONE, 11L),
				command("삼겹살", IngredientCategoryCode.MEAT, LocalDate.of(2026, 3, 6), null, false,
						new BigDecimal("0.6"), 12L)));

		assertThat(ids).hasSize(2);
		Ingredient milk = getUseCase.getIngredient(USER, ids.get(0));
		Ingredient pork = getUseCase.getIngredient(USER, ids.get(1));
		assertThat(milk.name()).isEqualTo("우유");
		assertThat(milk.receiptItemId()).isEqualTo(11L);
		// 달걀·유제품의 기본 보관 일수 10일
		assertThat(milk.expirationDate()).isEqualTo(LocalDate.of(2026, 3, 12));
		assertThat(pork.name()).isEqualTo("삼겹살");
		assertThat(pork.receiptItemId()).isEqualTo(12L);
	}

	@Test
	void 여러_건_중_하나가_실패하면_호출한_쪽의_트랜잭션이_롤백되어_하나도_남지_않는다() {
		List<RegisterIngredientCommand> commands = List.of(
				command("우유", IngredientCategoryCode.DAIRY_EGG, LocalDate.of(2026, 3, 12), null, false, BigDecimal.ONE,
						11L),
				// 수량이 0이라 규칙을 어긴다
				command("두부", IngredientCategoryCode.BEAN_TOFU, LocalDate.of(2026, 3, 8), null, false, BigDecimal.ZERO,
						12L));

		// 영수증 확정처럼 호출한 쪽이 연 트랜잭션 안에서 부른다.
		assertThatThrownBy(() -> transactionTemplate.execute(status -> registerUseCase.registerIngredients(commands)))
				.isInstanceOf(BusinessException.class);

		assertThat(countIngredients()).isZero();
	}

	@Test
	void 규칙을_어기면_INVALID_INPUT이다() {
		assertErrorCode(() -> registerUseCase.registerIngredient(command("우유", IngredientCategoryCode.DAIRY_EGG,
				LocalDate.of(2026, 3, 12), null, false, new BigDecimal("-1"), null)), ErrorCode.INVALID_INPUT);
		assertErrorCode(() -> registerUseCase.registerIngredient(command("  ", IngredientCategoryCode.DAIRY_EGG,
				LocalDate.of(2026, 3, 12), null, false, BigDecimal.ONE, null)), ErrorCode.INVALID_INPUT);
		assertErrorCode(() -> registerUseCase.registerIngredient(command("우유", IngredientCategoryCode.DAIRY_EGG,
				LocalDate.of(2026, 3, 12), null, false, new BigDecimal("0.0001"), null)), ErrorCode.INVALID_INPUT);
		assertThat(countIngredients()).isZero();
	}

	@Test
	void 카테고리가_없으면_INVALID_INPUT이다() {
		assertErrorCode(() -> registerUseCase.registerIngredient(command("우유", null, LocalDate.of(2026, 3, 12), null,
				false, BigDecimal.ONE, null)), ErrorCode.INVALID_INPUT);
	}

	@Test
	void 등록할_재료_목록이_없으면_INVALID_INPUT이고_빈_목록은_빈_결과이다() {
		assertErrorCode(() -> registerUseCase.registerIngredients(null), ErrorCode.INVALID_INPUT);
		assertThat(registerUseCase.registerIngredients(List.of())).isEmpty();
	}
}
