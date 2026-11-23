package com.remake.mealplan.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

// 도메인 규칙: Spring 없이 확인한다.
class MealPlanTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	private static MealPlan request(LocalDate startDate, Set<MealType> mealTypes, int servings) {
		return MealPlan.request(1L, startDate, MealPurpose.BALANCED, mealTypes, servings, TODAY);
	}

	private static MealPlan request() {
		return request(TODAY, EnumSet.of(MealType.LUNCH, MealType.DINNER), 2);
	}

	@Test
	void 요청하면_생성_중이고_만든_일수는_비어_있다() {
		MealPlan mealPlan = request();

		assertThat(mealPlan.status()).isEqualTo(MealPlanStatus.GENERATING);
		assertThat(mealPlan.generatedDays()).isNull();
		assertThat(mealPlan.endDate()).isEmpty();
	}

	@Test
	void 시작일은_오늘부터_고를_수_있다() {
		assertThat(request(TODAY, EnumSet.of(MealType.DINNER), 1).startDate()).isEqualTo(TODAY);
		assertThatThrownBy(() -> request(TODAY.minusDays(1), EnumSet.of(MealType.DINNER), 1))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 끼니가_없으면_거절한다() {
		assertThatThrownBy(() -> request(TODAY, EnumSet.noneOf(MealType.class), 1))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> request(TODAY, null, 1)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 인원은_1명_이상이다() {
		assertThat(request(TODAY, EnumSet.of(MealType.DINNER), 1).servings()).isEqualTo(1);
		assertThatThrownBy(() -> request(TODAY, EnumSet.of(MealType.DINNER), 0))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 끼니_목록은_밖에서_바꿀_수_없다() {
		Set<MealType> given = EnumSet.of(MealType.DINNER);
		MealPlan mealPlan = request(TODAY, given, 1);

		given.add(MealType.BREAKFAST);

		assertThat(mealPlan.mealTypes()).containsExactly(MealType.DINNER);
		assertThatThrownBy(() -> mealPlan.mealTypes().add(MealType.LUNCH))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void 완성하면_만든_일수와_마지막_날이_정해진다() {
		MealPlan mealPlan = request();

		mealPlan.complete(5);

		assertThat(mealPlan.status()).isEqualTo(MealPlanStatus.COMPLETED);
		assertThat(mealPlan.generatedDays()).isEqualTo(5);
		assertThat(mealPlan.endDate()).contains(LocalDate.of(2026, 3, 8));
	}

	@Test
	void 하루치만_만들면_마지막_날은_시작일이다() {
		MealPlan mealPlan = request();

		mealPlan.complete(1);

		assertThat(mealPlan.endDate()).contains(TODAY);
	}

	@Test
	void 만든_일수는_1일에서_7일이다() {
		assertThatThrownBy(() -> request().complete(0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> request().complete(MealPlan.MAX_DAYS + 1))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 실패하면_만든_일수_없이_실패_상태가_된다() {
		MealPlan mealPlan = request();

		mealPlan.fail();

		assertThat(mealPlan.status()).isEqualTo(MealPlanStatus.FAILED);
		assertThat(mealPlan.generatedDays()).isNull();
		assertThat(mealPlan.endDate()).isEmpty();
	}

	@Test
	void 이미_끝난_식단표는_다시_끝낼_수_없다() {
		MealPlan completed = request();
		completed.complete(3);
		MealPlan failed = request();
		failed.fail();

		assertThatThrownBy(() -> completed.complete(3)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(completed::fail).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> failed.complete(3)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(failed::fail).isInstanceOf(IllegalStateException.class);
	}
}
