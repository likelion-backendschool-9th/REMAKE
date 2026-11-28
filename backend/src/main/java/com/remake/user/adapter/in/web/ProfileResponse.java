package com.remake.user.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.remake.user.application.port.in.GetProfileUseCase;
import com.remake.user.domain.AuthProvider;
import com.remake.user.domain.UserPlan;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProfileResponse(
		AuthProvider provider,
		UserPlan plan,
		@Schema(nullable = true) Integer householdSize,
		int excludedIngredientCount,
		boolean recipeSupplementEnabled,
		boolean notificationEnabled,
		@Schema(nullable = true, allowableValues = {"BALANCED", "DIET", "HIGH_PROTEIN", "SIMPLE", "SAVING"})
		String lastMealPurpose,
		boolean onboardingCompleted) {

	static ProfileResponse from(GetProfileUseCase.Profile profile) {
		return new ProfileResponse(
				profile.provider(),
				profile.plan(),
				profile.householdSize(),
				profile.excludedIngredientCount(),
				profile.recipeSupplementEnabled(),
				profile.notificationEnabled(),
				profile.lastMealPurpose(),
				profile.onboardingCompleted());
	}
}
