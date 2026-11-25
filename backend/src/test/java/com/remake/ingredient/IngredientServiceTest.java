package com.remake.ingredient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
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
import com.remake.ingredient.domain.IngredientDetail;
import com.remake.ingredient.domain.Ingredient;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;
import com.remake.support.IntegrationTestSupport;

// 재료 상세·수정·삭제를 DB까지 확인한다.
class IngredientServiceTest extends IntegrationTestSupport {

	private static final LocalDate EXPIRATION = LocalDate.of(2026, 3, 10);

	private Long owner;
	private Long otherUser;

	@Autowired
	private GetIngredientUseCase getUseCase;

	@Autowired
	private UpdateIngredientUseCase updateUseCase;

	@Autowired
	private DeleteIngredientUseCase deleteUseCase;

	@BeforeEach
	void setUpUsers() {
		owner = createUser();
		otherUser = createUser();
	}

	private Long createMilk(Long userId, Long price) {
		return createIngredient(userId, "DAIRY_EGG", "우유", EXPIRATION, price);
	}

	/** 사전 단위(PACK)로 등록된 우유. 이름을 고치면 단위도 다시 정해지는 재료다 */
	private Long createDictionaryMilk(Long userId) {
		Long id = createMilk(userId, null);
		jdbcTemplate.update("UPDATE ingredients SET unit = 'PACK' WHERE id = ?", id);
		return id;
	}

	/** 장바구니·영수증 예외처럼 사전 단위와 다른 단위로 들어온 재료 */
	private Long createExceptionIngredient(Long userId, String categoryCode, String name, String unit) {
		Long id = createIngredient(userId, categoryCode, name, EXPIRATION, null);
		jdbcTemplate.update("UPDATE ingredients SET amount = 200, initial_amount = 200, unit = ? WHERE id = ?", unit, id);
		return id;
	}

	private void assertNotFound(Runnable call) {
		assertThatThrownBy(call::run)
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INGREDIENT_NOT_FOUND);
	}

	@Test
	void 본인_재료의_상세를_읽는다() {
		Long id = createMilk(owner, 3000L);

		Ingredient ingredient = getUseCase.getIngredient(owner, id).fridgeIngredient().ingredient();

		assertThat(ingredient.id()).isEqualTo(id);
		assertThat(ingredient.name()).isEqualTo("우유");
		assertThat(ingredient.expirationDate()).isEqualTo(EXPIRATION);
		assertThat(ingredient.price()).isEqualTo(3000L);
	}

	@Test
	void 다른_사용자의_재료는_없는_것과_같이_404이다() {
		Long id = createMilk(otherUser, null);

		assertNotFound(() -> getUseCase.getIngredient(owner, id).fridgeIngredient().ingredient());
	}

	@Test
	void 없는_재료는_404이다() {
		assertNotFound(() -> getUseCase.getIngredient(owner, 999_999L).fridgeIngredient().ingredient());
	}

	@Test
	void 소비기한을_고치면_응답용_D_day와_상태도_새_날짜로_계산한다() {
		Long id = createMilk(owner, null);

		IngredientDetail updated = updateUseCase.updateIngredient(
				new UpdateIngredientCommand(owner, id, null, null, LocalDate.of(2026, 3, 20), null, null));

		// 시험용 시계의 오늘은 2026-03-04이다
		assertThat(updated.fridgeIngredient().ingredient().expirationDate()).isEqualTo(LocalDate.of(2026, 3, 20));
		assertThat(updated.fridgeIngredient().dDay()).isEqualTo(16);
		assertThat(updated.fridgeIngredient().status()).isEqualTo(ExpiryStatus.OK);
		assertThat(updated.fridgeIngredient().category().code()).isEqualTo(IngredientCategoryCode.DAIRY_EGG);
	}

	@Test
	void 보관_위치와_가격을_고치고_가격을_지운다() {
		Long id = createMilk(owner, 3000L);

		updateUseCase.updateIngredient(
				new UpdateIngredientCommand(owner, id, null, null, null, StorageLocation.FREEZER, new PriceChange(2500L)));
		Ingredient changed = getUseCase.getIngredient(owner, id).fridgeIngredient().ingredient();
		assertThat(changed.storage()).isEqualTo(StorageLocation.FREEZER);
		assertThat(changed.price()).isEqualTo(2500L);

		updateUseCase.updateIngredient(new UpdateIngredientCommand(owner, id, null, null, null, null, new PriceChange(null)));
		assertThat(getUseCase.getIngredient(owner, id).fridgeIngredient().ingredient().price()).isNull();
	}

	@Test
	void 값을_넘기지_않은_항목은_그대로_둔다() {
		Long id = createMilk(owner, 3000L);

		updateUseCase.updateIngredient(new UpdateIngredientCommand(owner, id, null, null, LocalDate.of(2026, 4, 1), null, null));

		Ingredient ingredient = getUseCase.getIngredient(owner, id).fridgeIngredient().ingredient();
		assertThat(ingredient.storage()).isEqualTo(StorageLocation.FRIDGE);
		assertThat(ingredient.price()).isEqualTo(3000L);
	}

	@Test
	void 규칙을_어긴_수정은_INVALID_INPUT이고_값이_바뀌지_않는다() {
		Long id = createMilk(owner, 3000L);

		assertThatThrownBy(() -> updateUseCase.updateIngredient(
				new UpdateIngredientCommand(owner, id, null, null, LocalDate.of(2026, 4, 1), null, new PriceChange(-1L))))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_INPUT);

		Ingredient ingredient = getUseCase.getIngredient(owner, id).fridgeIngredient().ingredient();
		assertThat(ingredient.expirationDate()).isEqualTo(EXPIRATION);
		assertThat(ingredient.price()).isEqualTo(3000L);
	}

	@Test
	void 이름과_카테고리를_고친다() {
		Long id = createMilk(owner, null);

		updateUseCase.updateIngredient(new UpdateIngredientCommand(owner, id, "  두부  ", IngredientCategoryCode.BEAN_TOFU,
				null, null, null));

		Ingredient changed = getUseCase.getIngredient(owner, id).fridgeIngredient().ingredient();
		assertThat(changed.name()).isEqualTo("두부");
		assertThat(changed.categoryId()).isEqualTo(categoryId("BEAN_TOFU"));
	}

	@Test
	void 규칙을_어긴_이름과_없는_카테고리는_거부하고_값이_바뀌지_않는다() {
		Long id = createMilk(owner, null);

		assertThatThrownBy(() -> updateUseCase.updateIngredient(
				new UpdateIngredientCommand(owner, id, "가".repeat(51), null, null, null, null)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_INPUT);

		assertThat(getUseCase.getIngredient(owner, id).fridgeIngredient().ingredient().name()).isEqualTo("우유");
	}

	@Test
	void 수량을_고치면_처음_양도_바뀌고_사용한_재료는_거부한다() {
		Long id = createMilk(owner, null);

		updateUseCase.updateIngredient(new UpdateIngredientCommand(owner, id, null, null, null, null, null,
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
				""", owner, id, java.time.LocalDateTime.now(clock));
		assertThatThrownBy(() -> updateUseCase.updateIngredient(new UpdateIngredientCommand(owner, id, null, null, null,
				null, null, new java.math.BigDecimal("9"))))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_INPUT);
		assertThat(jdbcTemplate.queryForObject("SELECT initial_amount FROM ingredients WHERE id = ?",
				java.math.BigDecimal.class, id)).isEqualByComparingTo("5");
	}

	@Test
	void 다른_사용자의_재료는_고칠_수_없다() {
		Long id = createMilk(otherUser, 3000L);

		assertNotFound(() -> updateUseCase.updateIngredient(
				new UpdateIngredientCommand(owner, id, null, null, LocalDate.of(2026, 4, 1), null, null)));

		assertThat(getUseCase.getIngredient(otherUser, id).fridgeIngredient().ingredient().expirationDate()).isEqualTo(EXPIRATION);
	}

	@Test
	void 본인_재료를_삭제한다() {
		Long id = createMilk(owner, null);

		deleteUseCase.deleteIngredient(owner, id);

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredients WHERE id = ?", Long.class, id))
				.isZero();
	}

	@Test
	void 다른_사용자의_재료는_삭제할_수_없다() {
		Long id = createMilk(otherUser, null);

		assertNotFound(() -> deleteUseCase.deleteIngredient(owner, id));

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredients WHERE id = ?", Long.class, id))
				.isEqualTo(1L);
	}

	@Test
	void 사전_단위_재료는_이름을_바꾸면_단위만_다시_정하고_수량_숫자는_그대로다() {
		Long id = createDictionaryMilk(owner);
		jdbcTemplate.update("UPDATE ingredients SET amount = 3, initial_amount = 3 WHERE id = ?", id);

		IngredientDetail updated = updateUseCase.updateIngredient(
				new UpdateIngredientCommand(owner, id, "대파", null, null, null, null));

		// 우유(사전 단위 PACK) → 대파: 사전에 있는 이름이라 BUNCH
		assertThat(updated.fridgeIngredient().ingredient().unit()).isEqualTo(IngredientUnit.BUNCH);
		assertThat(updated.fridgeIngredient().ingredient().amount()).isEqualByComparingTo("3");
		assertThat(updated.fridgeIngredient().ingredient().initialAmount()).isEqualByComparingTo("3");
	}

	@Test
	void 사용_기록이_있어도_사전_단위_재료는_이름을_바꾸면_단위를_다시_정하고_기록은_그대로다() {
		Long id = createDictionaryMilk(owner);
		jdbcTemplate.update("""
				INSERT INTO ingredient_logs (user_id, ingredient_id, type, name, amount, unit, created_at)
				VALUES (?, ?, 'USED', '우유', 1, 'EA', ?)
				""", owner, id, java.time.LocalDateTime.now(clock));

		IngredientDetail updated = updateUseCase.updateIngredient(
				new UpdateIngredientCommand(owner, id, "대파", null, null, null, null));

		assertThat(updated.fridgeIngredient().ingredient().unit()).isEqualTo(IngredientUnit.BUNCH);
		assertThat(jdbcTemplate.queryForMap("SELECT name, unit FROM ingredient_logs WHERE ingredient_id = ?", id))
				.containsEntry("name", "우유").containsEntry("unit", "EA");
	}

	@Test
	void 카테고리만_바꿔도_저장될_단위가_미리보기와_같도록_다시_정한다() {
		// 같은 카테고리의 사전 이름("우유")이 들어 있는 이름은 그 카테고리일 때만 PACK이다
		Long id = createIngredient(owner, "VEGETABLE", "유기농 우유", EXPIRATION, null);

		IngredientDetail updated = updateUseCase.updateIngredient(
				new UpdateIngredientCommand(owner, id, null, IngredientCategoryCode.DAIRY_EGG, null, null, null));

		assertThat(updated.fridgeIngredient().ingredient().unit()).isEqualTo(IngredientUnit.PACK);
	}

	@Test
	void 이름과_카테고리를_바꾸지_않으면_단위를_건드리지_않는다() {
		Long id = createMilk(owner, null);

		// 같은 이름·같은 카테고리를 그대로 보내도, 다른 값만 고쳐도 단위는 그대로(시험 데이터는 EA)
		IngredientDetail same = updateUseCase.updateIngredient(
				new UpdateIngredientCommand(owner, id, "우유", IngredientCategoryCode.DAIRY_EGG, null, null, null));
		IngredientDetail other = updateUseCase.updateIngredient(
				new UpdateIngredientCommand(owner, id, null, null, null, StorageLocation.FREEZER, null));

		assertThat(same.fridgeIngredient().ingredient().unit()).isEqualTo(IngredientUnit.EA);
		assertThat(other.fridgeIngredient().ingredient().unit()).isEqualTo(IngredientUnit.EA);
	}

	@Test
	void 장바구니에서_온_재료는_이름을_바꿔도_단위와_수량이_그대로다() {
		// 장바구니 이동은 담긴 수량·단위(양파 200g)를 그대로 등록한다. 사전 단위(EA)와 다르다
		Long id = createExceptionIngredient(owner, "VEGETABLE", "양파", "G");

		IngredientDetail updated = updateUseCase.updateIngredient(
				new UpdateIngredientCommand(owner, id, "햇양파", null, null, null, null));

		assertThat(updated.fridgeIngredient().ingredient().name()).isEqualTo("햇양파");
		assertThat(updated.fridgeIngredient().ingredient().unit()).isEqualTo(IngredientUnit.G);
		assertThat(updated.fridgeIngredient().ingredient().amount()).isEqualByComparingTo("200");
		assertThat(updated.unitFollowsName()).isFalse();
	}

	@Test
	void 영수증_PACK_재료는_이름과_카테고리를_바꿔도_PACK_그대로다() {
		Long id = createExceptionIngredient(owner, "MEAT", "삼겹살", "PACK");

		IngredientDetail renamed = updateUseCase.updateIngredient(
				new UpdateIngredientCommand(owner, id, "목살", null, null, null, null));
		IngredientDetail recategorized = updateUseCase.updateIngredient(
				new UpdateIngredientCommand(owner, id, null, IngredientCategoryCode.PROCESSED, null, null, null));

		assertThat(renamed.fridgeIngredient().ingredient().unit()).isEqualTo(IngredientUnit.PACK);
		assertThat(recategorized.fridgeIngredient().ingredient().unit()).isEqualTo(IngredientUnit.PACK);
		assertThat(recategorized.fridgeIngredient().ingredient().amount()).isEqualByComparingTo("200");
	}

	@Test
	void 단위를_유지하는_재료도_사용_기록이_있으면_같은_규칙이다() {
		Long id = createExceptionIngredient(owner, "VEGETABLE", "양파", "G");
		jdbcTemplate.update("""
				INSERT INTO ingredient_logs (user_id, ingredient_id, type, name, amount, unit, created_at)
				VALUES (?, ?, 'USED', '양파', 50, 'G', ?)
				""", owner, id, java.time.LocalDateTime.now(clock));

		IngredientDetail updated = updateUseCase.updateIngredient(
				new UpdateIngredientCommand(owner, id, "햇양파", null, null, null, null));

		assertThat(updated.fridgeIngredient().ingredient().unit()).isEqualTo(IngredientUnit.G);
		assertThat(jdbcTemplate.queryForMap("SELECT name, unit FROM ingredient_logs WHERE ingredient_id = ?", id))
				.containsEntry("name", "양파").containsEntry("unit", "G");
	}

	@Test
	void 상세_응답은_이름을_고치면_단위도_다시_정해지는지_알려_준다() {
		Long dictionary = createDictionaryMilk(owner);
		Long exception = createExceptionIngredient(owner, "VEGETABLE", "양파", "G");

		assertThat(getUseCase.getIngredient(owner, dictionary).unitFollowsName()).isTrue();
		assertThat(getUseCase.getIngredient(owner, exception).unitFollowsName()).isFalse();
	}

	@Test
	void 수정_뒤에도_바뀐_이름과_단위_기준으로_알려_준다() {
		Long id = createDictionaryMilk(owner);

		IngredientDetail updated = updateUseCase.updateIngredient(
				new UpdateIngredientCommand(owner, id, "대파", null, null, null, null));

		// 대파(BUNCH)는 사전 단위 그대로라 계속 다시 정해지는 재료다
		assertThat(updated.unitFollowsName()).isTrue();
	}
}
