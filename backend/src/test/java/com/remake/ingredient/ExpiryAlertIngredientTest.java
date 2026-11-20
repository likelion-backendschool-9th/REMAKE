package com.remake.ingredient;

import static com.remake.ingredient.domain.IngredientStatus.ACTIVE;
import static com.remake.ingredient.domain.IngredientStatus.FINISHED;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.ingredient.application.port.in.GetExpiryAlertIngredientsUseCase;
import com.remake.ingredient.domain.ExpiredIngredient;
import com.remake.ingredient.domain.ExpiredIngredientCount;
import com.remake.ingredient.domain.ExpiringIngredient;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.support.IntegrationTestSupport;
import com.remake.support.MutableClock;

// 알림 배치가 부르는 UseCase를 DB까지 확인한다. 기본 시각은 2026-03-04 10:00(서울)이라 오늘은 3월 4일이다.
// 사용자 ID(101, 102)는 users 테이블이 아직 없어 임의 번호를 쓴다. users 마이그레이션이 ingredients.user_id 외래 키를
// 추가하면 이 테스트가 깨지므로, 그 PR에서 IntegrationTestSupport의 createUser()로 사용자를 만들어 쓰도록 바꾼다.
class ExpiryAlertIngredientTest extends IntegrationTestSupport {

	private static final Long USER_1 = 101L;
	private static final Long USER_2 = 102L;

	@Autowired
	private GetExpiryAlertIngredientsUseCase useCase;

	@Test
	void 임박은_오늘부터_3일_뒤까지이고_D_DAY를_포함한다() {
		Long dDay = createIngredient(USER_1, "MEAT", "닭가슴살", LocalDate.of(2026, 3, 4), ACTIVE);
		Long d1 = createIngredient(USER_1, "VEGETABLE", "양파", LocalDate.of(2026, 3, 5), ACTIVE);
		Long d3 = createIngredient(USER_1, "DAIRY_EGG", "우유", LocalDate.of(2026, 3, 7), ACTIVE);
		createIngredient(USER_1, "VEGETABLE", "당근", LocalDate.of(2026, 3, 8), ACTIVE); // D-4: 아직 임박 아님
		createIngredient(USER_1, "VEGETABLE", "시금치", LocalDate.of(2026, 3, 3), ACTIVE); // 어제 만료: 임박이 아니라 만료

		assertThat(useCase.getExpiringIngredients())
				.extracting(ExpiringIngredient::ingredientId)
				.containsExactly(dDay, d1, d3);
	}

	@Test
	void 임박_재료는_알림에_쓸_값을_담는다() {
		Long id = createIngredient(USER_1, "DAIRY_EGG", "우유", LocalDate.of(2026, 3, 6), ACTIVE);

		assertThat(useCase.getExpiringIngredients())
				.containsExactly(new ExpiringIngredient(USER_1, id, "우유", LocalDate.of(2026, 3, 6)));
	}

	@Test
	void 임박은_사용자별로_모아_소비기한_순으로_돌려준다() {
		Long user2Late = createIngredient(USER_2, "VEGETABLE", "파", LocalDate.of(2026, 3, 6), ACTIVE);
		Long user1Late = createIngredient(USER_1, "VEGETABLE", "양파", LocalDate.of(2026, 3, 6), ACTIVE);
		Long user1Early = createIngredient(USER_1, "MEAT", "돼지고기", LocalDate.of(2026, 3, 5), ACTIVE);

		assertThat(useCase.getExpiringIngredients())
				.extracting(ExpiringIngredient::ingredientId)
				.containsExactly(user1Early, user1Late, user2Late);
	}

	@Test
	void 다_쓴_재료는_임박에도_만료에도_포함하지_않는다() {
		createIngredient(USER_1, "MEAT", "닭가슴살", LocalDate.of(2026, 3, 5), FINISHED);
		createIngredient(USER_1, "MEAT", "소고기", LocalDate.of(2026, 3, 1), FINISHED);

		assertThat(useCase.getExpiringIngredients()).isEmpty();
		assertThat(useCase.getExpiredIngredientCounts()).isEmpty();
		assertThat(useCase.getIngredientsExpiredYesterday()).isEmpty();
	}

	@Test
	void 만료는_소비기한이_오늘보다_이전인_재료를_사용자별로_센다() {
		createIngredient(USER_1, "VEGETABLE", "상추", LocalDate.of(2026, 3, 3), ACTIVE);
		createIngredient(USER_1, "VEGETABLE", "깻잎", LocalDate.of(2026, 2, 20), ACTIVE);
		createIngredient(USER_2, "MEAT", "닭고기", LocalDate.of(2026, 3, 1), ACTIVE);
		createIngredient(USER_1, "VEGETABLE", "오늘이_기한", LocalDate.of(2026, 3, 4), ACTIVE); // D-DAY는 만료가 아니다

		assertThat(useCase.getExpiredIngredientCounts())
				.containsExactly(new ExpiredIngredientCount(USER_1, 2), new ExpiredIngredientCount(USER_2, 1));
	}

	@Test
	void 어제_만료된_재료만_골라_이벤트_속성을_담는다() {
		Long withPrice = createIngredient(USER_1, "MEAT", "삼겹살", LocalDate.of(2026, 3, 3), ACTIVE, 12000L);
		Long noPrice = createIngredient(USER_2, "SEASONING", "간장", LocalDate.of(2026, 3, 3), ACTIVE);
		createIngredient(USER_1, "VEGETABLE", "그저께_만료", LocalDate.of(2026, 3, 2), ACTIVE);
		createIngredient(USER_1, "VEGETABLE", "오늘이_기한", LocalDate.of(2026, 3, 4), ACTIVE);

		assertThat(useCase.getIngredientsExpiredYesterday()).containsExactly(
				new ExpiredIngredient(USER_1, withPrice, IngredientCategoryCode.MEAT, true),
				new ExpiredIngredient(USER_2, noPrice, IngredientCategoryCode.SEASONING, false));
	}

	@Test
	void 자정이_지나면_기준_날짜가_바뀐다() {
		Long id = createIngredient(USER_1, "VEGETABLE", "양파", LocalDate.of(2026, 3, 7), ACTIVE);

		// 3월 4일 23:59: 3월 7일은 D-3이라 임박이고, 아직 만료가 아니다.
		clock.setNow(ZonedDateTime.of(2026, 3, 4, 23, 59, 0, 0, MutableClock.ZONE));
		assertThat(useCase.getExpiringIngredients()).extracting(ExpiringIngredient::ingredientId).containsExactly(id);

		// 3월 7일 00:00: D-DAY라서 여전히 임박이고, 만료는 아니다.
		clock.setNow(ZonedDateTime.of(2026, 3, 7, 0, 0, 0, 0, MutableClock.ZONE));
		assertThat(useCase.getExpiringIngredients()).extracting(ExpiringIngredient::ingredientId).containsExactly(id);
		assertThat(useCase.getExpiredIngredientCounts()).isEmpty();

		// 3월 8일 00:00: 하루가 지나 임박에서 빠지고 만료로 넘어가며, 어제 만료 목록에 들어온다.
		clock.setNow(ZonedDateTime.of(2026, 3, 8, 0, 0, 0, 0, MutableClock.ZONE));
		assertThat(useCase.getExpiringIngredients()).isEmpty();
		assertThat(useCase.getExpiredIngredientCounts()).containsExactly(new ExpiredIngredientCount(USER_1, 1));
		assertThat(useCase.getIngredientsExpiredYesterday()).extracting(ExpiredIngredient::ingredientId)
				.containsExactly(id);

		// 3월 9일: 어제 만료 목록에서는 빠지고 만료 개수에만 남는다. 재료당 한 번만 이벤트가 나가는 근거다.
		clock.setNow(ZonedDateTime.of(2026, 3, 9, 0, 0, 0, 0, MutableClock.ZONE));
		assertThat(useCase.getIngredientsExpiredYesterday()).isEmpty();
		assertThat(useCase.getExpiredIngredientCounts()).containsExactly(new ExpiredIngredientCount(USER_1, 1));
	}
}
