package com.remake.ingredient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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
class IngredientRegistrationTest extends IntegrationTestSupport {

	private Long userId;

	@Autowired
	private RegisterIngredientUseCase registerUseCase;

	@Autowired
	private GetIngredientUseCase getUseCase;

	@BeforeEach
	void setUpUser() {
		userId = createUser();
	}

	private RegisterIngredientCommand command(String name, IngredientCategoryCode category, LocalDate expirationDate,
			LocalDate baseDate, BigDecimal amount, Long receiptItemId) {
		return new RegisterIngredientCommand(userId, name, category, StorageLocation.FRIDGE, expirationDate, baseDate,
				amount, IngredientUnit.EA, 3000L, receiptItemId);
	}

	// 단위를 null로 넘기면 서버가 정한다(직접 추가·장바구니). 넣으면 그대로 쓴다(영수증 PACK 예외).
	private RegisterIngredientCommand commandWithUnit(String name, IngredientCategoryCode category, IngredientUnit unit) {
		return new RegisterIngredientCommand(userId, name, category, StorageLocation.FRIDGE, LocalDate.of(2026, 3, 12), null,
				BigDecimal.ONE, unit, null, null);
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
	void 직접_추가하면_ingredient_added_이벤트가_MANUAL로_남고_이름은_넣지_않는다() {
		transactionTemplate.executeWithoutResult(status -> registerUseCase.registerIngredient(
				command("우유", IngredientCategoryCode.DAIRY_EGG, LocalDate.of(2026, 3, 12), null,
						new BigDecimal("2"), null)));
		awaitEventWrites();

		assertThat(jdbcTemplate.queryForObject("SELECT event_name FROM events", String.class)).isEqualTo("ingredient_added");
		assertThat(jdbcTemplate.queryForObject("SELECT user_id FROM events", Long.class)).isEqualTo(userId);
		String props = jdbcTemplate.queryForObject("SELECT properties::text FROM events", String.class);
		assertThat(props).contains("MANUAL").contains("DAIRY_EGG").contains("FRIDGE").doesNotContain("우유");
	}

	@Test
	void 영수증_품목이면_RECEIPT로_남고_여러_건이면_건마다_하나씩_남는다() {
		transactionTemplate.executeWithoutResult(status -> registerUseCase.registerIngredients(List.of(
				command("우유", IngredientCategoryCode.DAIRY_EGG, null, LocalDate.of(2026, 3, 1), BigDecimal.ONE, createReceiptItem(userId, "우유", "DAIRY_EGG")),
				command("두부", IngredientCategoryCode.BEAN_TOFU, null, LocalDate.of(2026, 3, 1), BigDecimal.ONE, createReceiptItem(userId, "두부", "BEAN_TOFU")))));
		awaitEventWrites();

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM events WHERE properties::text LIKE '%RECEIPT%'",
				Long.class)).isEqualTo(2L);
	}

	@Test
	void 등록이_롤백되면_이벤트도_남지_않는다() {
		// rollback-only는 예외 없이 끝난다. 등록이 예외를 던지면 그대로 테스트가 실패해야 하므로 삼키지 않는다.
		transactionTemplate.executeWithoutResult(status -> {
			registerUseCase.registerIngredient(command("우유", IngredientCategoryCode.DAIRY_EGG,
					LocalDate.of(2026, 3, 12), null, BigDecimal.ONE, null));
			// 롤백 전에 실제로 저장됐는지 확인한다. 저장이 안 돼서 0건인 경우와 롤백돼서 0건인 경우를 가른다.
			assertThat(countIngredients()).isOne();
			status.setRollbackOnly();
		});
		awaitEventWrites();

		assertThat(countIngredients()).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM events", Long.class)).isZero();
	}

	@Test
	void 직접_추가하면_입력한_값이_그대로_저장된다() {
		Long id = registerUseCase.registerIngredient(command("우유", IngredientCategoryCode.DAIRY_EGG,
				LocalDate.of(2026, 3, 12), null, new BigDecimal("2"), null));

		Ingredient saved = getUseCase.getIngredient(userId, id).ingredient();
		assertThat(saved.name()).isEqualTo("우유");
		assertThat(saved.categoryId()).isEqualTo(categoryId("DAIRY_EGG"));
		assertThat(saved.expirationDate()).isEqualTo(LocalDate.of(2026, 3, 12));
		assertThat(saved.amount()).isEqualByComparingTo("2");
		assertThat(saved.price()).isEqualTo(3000L);
		assertThat(saved.receiptItemId()).isNull();
	}

	@Test
	void 소비기한이_없으면_구매일에_기본_보관_일수를_더해_추정한다() {
		// 채소의 기본 보관 일수는 7일이다.
		Long id = registerUseCase.registerIngredient(command("양파", IngredientCategoryCode.VEGETABLE, null,
				LocalDate.of(2026, 3, 1), BigDecimal.ONE, null));

		Ingredient saved = getUseCase.getIngredient(userId, id).ingredient();
		assertThat(saved.expirationDate()).isEqualTo(LocalDate.of(2026, 3, 8));
	}

	@Test
	void 기준일이_없으면_오늘을_기준으로_추정한다() {
		Long id = registerUseCase.registerIngredient(command("양파", IngredientCategoryCode.VEGETABLE, null, null, BigDecimal.ONE, null));

		assertThat(getUseCase.getIngredient(userId, id).ingredient().expirationDate()).isEqualTo(LocalDate.of(2026, 3, 11));
	}

	@Test
	void 넘긴_소비기한이_있으면_기준일과_상관없이_그대로_저장한다() {
		Long id = registerUseCase.registerIngredient(command("양파", IngredientCategoryCode.VEGETABLE,
				LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 1), BigDecimal.ONE, createReceiptItem(userId, "양파", "VEGETABLE")));

		Ingredient saved = getUseCase.getIngredient(userId, id).ingredient();
		assertThat(saved.expirationDate()).isEqualTo(LocalDate.of(2026, 3, 9));
	}

	@Test
	void 영수증은_여러_건을_입력_순서대로_등록하고_품목을_연결한다() {
		Long milkItemId = createReceiptItem(userId, "우유", "DAIRY_EGG");
		Long porkItemId = createReceiptItem(userId, "삼겹살", "MEAT");
		List<Long> ids = registerUseCase.registerIngredients(List.of(
				command("우유", IngredientCategoryCode.DAIRY_EGG, null, LocalDate.of(2026, 3, 2), BigDecimal.ONE, milkItemId),
				command("삼겹살", IngredientCategoryCode.MEAT, LocalDate.of(2026, 3, 6), null,
						new BigDecimal("0.6"), porkItemId)));

		assertThat(ids).hasSize(2);
		Ingredient milk = getUseCase.getIngredient(userId, ids.get(0)).ingredient();
		Ingredient pork = getUseCase.getIngredient(userId, ids.get(1)).ingredient();
		assertThat(milk.name()).isEqualTo("우유");
		assertThat(milk.receiptItemId()).isEqualTo(milkItemId);
		// 달걀·유제품의 기본 보관 일수 10일
		assertThat(milk.expirationDate()).isEqualTo(LocalDate.of(2026, 3, 12));
		assertThat(pork.name()).isEqualTo("삼겹살");
		assertThat(pork.receiptItemId()).isEqualTo(porkItemId);
	}

	@Test
	void 여러_건_중_하나가_실패하면_호출한_쪽의_트랜잭션이_롤백되어_하나도_남지_않는다() {
		List<RegisterIngredientCommand> commands = List.of(
				command("우유", IngredientCategoryCode.DAIRY_EGG, LocalDate.of(2026, 3, 12), null, BigDecimal.ONE,
						createReceiptItem(userId, "우유", "DAIRY_EGG")),
				// 수량이 0이라 규칙을 어긴다
				command("두부", IngredientCategoryCode.BEAN_TOFU, LocalDate.of(2026, 3, 8), null, BigDecimal.ZERO,
						createReceiptItem(userId, "두부", "BEAN_TOFU")));

		// 영수증 확정처럼 호출한 쪽이 연 트랜잭션 안에서 부른다.
		assertThatThrownBy(() -> transactionTemplate.execute(status -> registerUseCase.registerIngredients(commands)))
				.isInstanceOf(BusinessException.class);

		assertThat(countIngredients()).isZero();
	}

	@Test
	void 규칙을_어기면_INVALID_INPUT이다() {
		assertErrorCode(() -> registerUseCase.registerIngredient(command("우유", IngredientCategoryCode.DAIRY_EGG,
				LocalDate.of(2026, 3, 12), null, new BigDecimal("-1"), null)), ErrorCode.INVALID_INPUT);
		assertErrorCode(() -> registerUseCase.registerIngredient(command("  ", IngredientCategoryCode.DAIRY_EGG,
				LocalDate.of(2026, 3, 12), null, BigDecimal.ONE, null)), ErrorCode.INVALID_INPUT);
		assertErrorCode(() -> registerUseCase.registerIngredient(command("우유", IngredientCategoryCode.DAIRY_EGG,
				LocalDate.of(2026, 3, 12), null, new BigDecimal("0.0001"), null)), ErrorCode.INVALID_INPUT);
		assertThat(countIngredients()).isZero();
	}

	@Test
	void 카테고리가_없으면_INVALID_INPUT이다() {
		assertErrorCode(() -> registerUseCase.registerIngredient(command("우유", null, LocalDate.of(2026, 3, 12), null, BigDecimal.ONE, null)), ErrorCode.INVALID_INPUT);
	}

	@Test
	void 등록할_재료_목록이_없으면_INVALID_INPUT이고_빈_목록은_빈_결과이다() {
		assertErrorCode(() -> registerUseCase.registerIngredients(null), ErrorCode.INVALID_INPUT);
		assertThat(registerUseCase.registerIngredients(List.of())).isEmpty();
	}

	@Test
	void 단위를_넘기지_않으면_서버가_재료명으로_정한다() {
		Long exact = registerUseCase.registerIngredient(commandWithUnit("대파", IngredientCategoryCode.VEGETABLE, null));
		Long contained = registerUseCase.registerIngredient(commandWithUnit("유기농 우유", IngredientCategoryCode.DAIRY_EGG, null));
		Long unknown = registerUseCase.registerIngredient(commandWithUnit("알수없는재료", IngredientCategoryCode.ETC, null));

		assertThat(getUseCase.getIngredient(userId, exact).ingredient().unit()).isEqualTo(IngredientUnit.BUNCH);
		assertThat(getUseCase.getIngredient(userId, contained).ingredient().unit()).isEqualTo(IngredientUnit.PACK);
		assertThat(getUseCase.getIngredient(userId, unknown).ingredient().unit()).isEqualTo(IngredientUnit.EA);
	}

	@Test
	void 호출한_기능이_단위를_넘기면_그대로_쓴다() {
		// 영수증에서 무게를 읽지 못한 품목의 PACK 예외(#145)처럼 호출한 쪽이 정한 단위다
		Long id = registerUseCase.registerIngredient(commandWithUnit("한돈 앞다리 돼지고기", IngredientCategoryCode.MEAT,
				IngredientUnit.PACK));

		assertThat(getUseCase.getIngredient(userId, id).ingredient().unit()).isEqualTo(IngredientUnit.PACK);
	}

	@Test
	void 재료명이_비어_있으면_단위를_정하지_못해_INVALID_INPUT이다() {
		assertErrorCode(() -> registerUseCase.registerIngredient(commandWithUnit("  ", IngredientCategoryCode.ETC, null)),
				ErrorCode.INVALID_INPUT);
	}
}
