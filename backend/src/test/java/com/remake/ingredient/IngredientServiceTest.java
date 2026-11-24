package com.remake.ingredient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.DeleteIngredientUseCase;
import com.remake.ingredient.application.port.in.GetIngredientUseCase;
import com.remake.ingredient.application.port.in.UpdateIngredientCommand;
import com.remake.ingredient.application.port.in.UpdateIngredientCommand.PriceChange;
import com.remake.ingredient.application.port.in.UpdateIngredientUseCase;
import com.remake.ingredient.domain.ExpiryStatus;
import com.remake.ingredient.domain.FridgeIngredient;
import com.remake.ingredient.domain.Ingredient;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.StorageLocation;
import com.remake.support.IntegrationTestSupport;

// 재료 상세·수정·삭제를 DB까지 확인한다. 사용자 ID(101, 102)는 users 테이블이 없어 임의 번호를 쓴다.
// users 마이그레이션이 외래 키를 추가하면 IntegrationTestSupport의 createUser()로 바꿔야 한다.
class IngredientServiceTest extends IntegrationTestSupport {

	private static final Long OWNER = 101L;
	private static final Long OTHER = 102L;
	private static final LocalDate EXPIRATION = LocalDate.of(2026, 3, 10);

	@Autowired
	private GetIngredientUseCase getUseCase;

	@Autowired
	private UpdateIngredientUseCase updateUseCase;

	@Autowired
	private DeleteIngredientUseCase deleteUseCase;

	private Long createMilk(Long userId, Long price) {
		return createIngredient(userId, "DAIRY_EGG", "우유", EXPIRATION, price);
	}

	private void assertNotFound(Runnable call) {
		assertThatThrownBy(call::run)
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INGREDIENT_NOT_FOUND);
	}

	@Test
	void 본인_재료의_상세를_읽는다() {
		Long id = createMilk(OWNER, 3000L);

		Ingredient ingredient = getUseCase.getIngredient(OWNER, id).ingredient();

		assertThat(ingredient.id()).isEqualTo(id);
		assertThat(ingredient.name()).isEqualTo("우유");
		assertThat(ingredient.expirationDate()).isEqualTo(EXPIRATION);
		assertThat(ingredient.price()).isEqualTo(3000L);
	}

	@Test
	void 다른_사용자의_재료는_없는_것과_같이_404이다() {
		Long id = createMilk(OTHER, null);

		assertNotFound(() -> getUseCase.getIngredient(OWNER, id).ingredient());
	}

	@Test
	void 없는_재료는_404이다() {
		assertNotFound(() -> getUseCase.getIngredient(OWNER, 999_999L).ingredient());
	}

	@Test
	void 소비기한을_고치면_응답용_D_day와_상태도_새_날짜로_계산한다() {
		Long id = createMilk(OWNER, null);

		FridgeIngredient updated = updateUseCase.updateIngredient(
				new UpdateIngredientCommand(OWNER, id, null, null, LocalDate.of(2026, 3, 20), null, null));

		// 시험용 시계의 오늘은 2026-03-04이다
		assertThat(updated.ingredient().expirationDate()).isEqualTo(LocalDate.of(2026, 3, 20));
		assertThat(updated.dDay()).isEqualTo(16);
		assertThat(updated.status()).isEqualTo(ExpiryStatus.OK);
		assertThat(updated.category().code()).isEqualTo(IngredientCategoryCode.DAIRY_EGG);
	}

	@Test
	void 보관_위치와_가격을_고치고_가격을_지운다() {
		Long id = createMilk(OWNER, 3000L);

		updateUseCase.updateIngredient(
				new UpdateIngredientCommand(OWNER, id, null, null, null, StorageLocation.FREEZER, new PriceChange(2500L)));
		Ingredient changed = getUseCase.getIngredient(OWNER, id).ingredient();
		assertThat(changed.storage()).isEqualTo(StorageLocation.FREEZER);
		assertThat(changed.price()).isEqualTo(2500L);

		updateUseCase.updateIngredient(new UpdateIngredientCommand(OWNER, id, null, null, null, null, new PriceChange(null)));
		assertThat(getUseCase.getIngredient(OWNER, id).ingredient().price()).isNull();
	}

	@Test
	void 값을_넘기지_않은_항목은_그대로_둔다() {
		Long id = createMilk(OWNER, 3000L);

		updateUseCase.updateIngredient(new UpdateIngredientCommand(OWNER, id, null, null, LocalDate.of(2026, 4, 1), null, null));

		Ingredient ingredient = getUseCase.getIngredient(OWNER, id).ingredient();
		assertThat(ingredient.storage()).isEqualTo(StorageLocation.FRIDGE);
		assertThat(ingredient.price()).isEqualTo(3000L);
	}

	@Test
	void 규칙을_어긴_수정은_INVALID_INPUT이고_값이_바뀌지_않는다() {
		Long id = createMilk(OWNER, 3000L);

		assertThatThrownBy(() -> updateUseCase.updateIngredient(
				new UpdateIngredientCommand(OWNER, id, null, null, LocalDate.of(2026, 4, 1), null, new PriceChange(-1L))))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_INPUT);

		Ingredient ingredient = getUseCase.getIngredient(OWNER, id).ingredient();
		assertThat(ingredient.expirationDate()).isEqualTo(EXPIRATION);
		assertThat(ingredient.price()).isEqualTo(3000L);
	}

	@Test
	void 이름과_카테고리를_고친다() {
		Long id = createMilk(OWNER, null);

		updateUseCase.updateIngredient(new UpdateIngredientCommand(OWNER, id, "  두부  ", IngredientCategoryCode.BEAN_TOFU,
				null, null, null));

		Ingredient changed = getUseCase.getIngredient(OWNER, id).ingredient();
		assertThat(changed.name()).isEqualTo("두부");
		assertThat(changed.categoryId()).isEqualTo(categoryId("BEAN_TOFU"));
	}

	@Test
	void 규칙을_어긴_이름과_없는_카테고리는_거부하고_값이_바뀌지_않는다() {
		Long id = createMilk(OWNER, null);

		assertThatThrownBy(() -> updateUseCase.updateIngredient(
				new UpdateIngredientCommand(OWNER, id, "가".repeat(51), null, null, null, null)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_INPUT);

		assertThat(getUseCase.getIngredient(OWNER, id).ingredient().name()).isEqualTo("우유");
	}

	@Test
	void 수량을_고치면_처음_양도_바뀌고_사용한_재료는_거부한다() {
		Long id = createMilk(OWNER, null);

		updateUseCase.updateIngredient(new UpdateIngredientCommand(OWNER, id, null, null, null, null, null,
				new java.math.BigDecimal("5")));
		assertThat(jdbcTemplate.queryForObject("SELECT amount FROM ingredients WHERE id = ?", java.math.BigDecimal.class, id))
				.isEqualByComparingTo("5");
		assertThat(jdbcTemplate.queryForObject("SELECT initial_amount FROM ingredients WHERE id = ?",
				java.math.BigDecimal.class, id)).isEqualByComparingTo("5");

		// 쓰거나 버린 기록이 생기면 수량을 고칠 수 없다.
		jdbcTemplate.update("UPDATE ingredients SET amount = 2 WHERE id = ?", id);
		jdbcTemplate.update("""
				INSERT INTO ingredient_logs (user_id, ingredient_id, type, name, amount, unit, created_at)
				VALUES (?, ?, 'USED', '사과', 3, 'EA', ?)
				""", OWNER, id, java.time.LocalDateTime.now(clock));
		assertThatThrownBy(() -> updateUseCase.updateIngredient(new UpdateIngredientCommand(OWNER, id, null, null, null,
				null, null, new java.math.BigDecimal("9"))))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_INPUT);
		assertThat(jdbcTemplate.queryForObject("SELECT initial_amount FROM ingredients WHERE id = ?",
				java.math.BigDecimal.class, id)).isEqualByComparingTo("5");
	}

	@Test
	void 다른_사용자의_재료는_고칠_수_없다() {
		Long id = createMilk(OTHER, 3000L);

		assertNotFound(() -> updateUseCase.updateIngredient(
				new UpdateIngredientCommand(OWNER, id, null, null, LocalDate.of(2026, 4, 1), null, null)));

		assertThat(getUseCase.getIngredient(OTHER, id).ingredient().expirationDate()).isEqualTo(EXPIRATION);
	}

	@Test
	void 본인_재료를_삭제한다() {
		Long id = createMilk(OWNER, null);

		deleteUseCase.deleteIngredient(OWNER, id);

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredients WHERE id = ?", Long.class, id))
				.isZero();
	}

	@Test
	void 다른_사용자의_재료는_삭제할_수_없다() {
		Long id = createMilk(OTHER, null);

		assertNotFound(() -> deleteUseCase.deleteIngredient(OWNER, id));

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredients WHERE id = ?", Long.class, id))
				.isEqualTo(1L);
	}
}
