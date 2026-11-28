package com.remake.quota.application.port.in;

import java.time.LocalDateTime;

/**
 * 결제 이후 프리미엄 혜택 사용량(#228·#343).
 * 식단표 생성 횟수와, BASIC 일일 한도를 넘는 AI 레시피 생성 횟수를 센다.
 */
public interface GetPremiumBenefitUsageSinceUseCase {

	BenefitUsage since(Long userId, LocalDateTime from);

	record BenefitUsage(int mealPlanGenerations, int recipeGenerationsAboveBasicDailyLimit) {

		public boolean unused() {
			return mealPlanGenerations == 0 && recipeGenerationsAboveBasicDailyLimit == 0;
		}
	}
}
