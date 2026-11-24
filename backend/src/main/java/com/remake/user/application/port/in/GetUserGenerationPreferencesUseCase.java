package com.remake.user.application.port.in;

import java.util.List;

import com.remake.user.domain.AllergenCode;

/**
 * AI 레시피·식단표 생성이 쓰는 회원 설정. 다른 영역은 이 UseCase만 호출한다.
 */
public interface GetUserGenerationPreferencesUseCase {

	record Preferences(
			Integer householdSize,
			boolean recipeSupplementEnabled,
			List<AllergenCode> excludedAllergenCodes) {
	}

	Preferences getPreferences(Long userId);
}
