package com.remake.user.adapter.in.web;

import com.remake.user.application.port.in.UpdateProfileUseCase;
import com.remake.user.domain.User;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 프로필 부분 수정. 보낸 필드만 바꾼다. 가구원 수를 보내면 1~50.
 */
public record UpdateProfileRequest(
		@Min(1) @Max(User.MAX_HOUSEHOLD_SIZE) Integer householdSize,
		Boolean recipeSupplementEnabled,
		Boolean notificationEnabled) {

	UpdateProfileUseCase.Command toCommand(Long userId) {
		return new UpdateProfileUseCase.Command(userId, householdSize, recipeSupplementEnabled, notificationEnabled);
	}
}
