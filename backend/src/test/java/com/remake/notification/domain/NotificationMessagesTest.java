package com.remake.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class NotificationMessagesTest {

	@Test
	void 임박_문구에_재료명이_들어간다() {
		assertThat(NotificationMessages.expirySoon("우유"))
				.isEqualTo("우유의 소비기한이 얼마 남지 않았어요. 제품의 포장지에서 사용 기한을 확인해 주세요");
	}

	@Test
	void 기한_지남_문구에_개수가_들어간다() {
		assertThat(NotificationMessages.expired(3))
				.isEqualTo("기한이 지난 재료 3개가 있어요. 냉장고를 확인해 주세요");
	}

	@Test
	void 식단표_완성_문구는_시작일과_일수를_쓴다() {
		assertThat(NotificationMessages.mealPlanCompleted(LocalDate.of(2026, 11, 24), 5, 5, 0))
				.isEqualTo("11월 24일부터 5일치 식단표가 완성됐어요");
	}

	@Test
	void 최대_일수를_채우면_장바구니에_담았어도_부족_문장을_붙이지_않는다() {
		assertThat(NotificationMessages.mealPlanCompleted(LocalDate.of(2026, 11, 24), 7, 7, 3))
				.isEqualTo("11월 24일부터 7일치 식단표가 완성됐어요");
	}

	@Test
	void 최대_일수를_못_채우면_N일치만_만들었다고_덧붙인다() {
		assertThat(NotificationMessages.mealPlanCompleted(LocalDate.of(2026, 11, 24), 2, 5, 0))
				.isEqualTo("11월 24일부터 2일치 식단표가 완성됐어요. 재료가 부족해 2일치만 만들었어요");
	}

	@Test
	void 최대_일수를_못_채우고_장바구니에_담았으면_담은_개수도_덧붙인다() {
		assertThat(NotificationMessages.mealPlanCompleted(LocalDate.of(2026, 11, 24), 2, 5, 3))
				.isEqualTo("11월 24일부터 2일치 식단표가 완성됐어요. 재료가 부족해 2일치만 만들었어요. "
						+ "부족한 재료 3개를 장바구니에 담았어요");
	}

	@Test
	void 식단표_실패_문구는_사유별로_다르다() {
		assertThat(NotificationMessages.mealPlanFailed(MealPlanFailReason.NO_INGREDIENTS))
				.contains("재료가 부족해");
		assertThat(NotificationMessages.mealPlanFailed(MealPlanFailReason.GENERIC))
				.contains("잠시 후 다시");
	}

	@Test
	void 개수가_1_미만이면_거부한다() {
		assertThatThrownBy(() -> NotificationMessages.expired(0))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
