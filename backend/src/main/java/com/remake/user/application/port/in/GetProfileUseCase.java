package com.remake.user.application.port.in;

import com.remake.user.domain.AuthProvider;
import com.remake.user.domain.UserPlan;

public interface GetProfileUseCase {

	record Profile(
			AuthProvider provider,
			UserPlan plan,
			Integer householdSize,
			int excludedIngredientCount,
			boolean recipeSupplementEnabled,
			boolean notificationEnabled,
			String lastMealPurpose,
			boolean onboardingCompleted) {
	}

	Profile getProfile(Long userId);
}
