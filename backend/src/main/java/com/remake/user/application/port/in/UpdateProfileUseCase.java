package com.remake.user.application.port.in;

public interface UpdateProfileUseCase {

	record Command(
			Long userId,
			Integer householdSize,
			Boolean recipeSupplementEnabled,
			Boolean notificationEnabled) {
	}

	GetProfileUseCase.Profile updateProfile(Command command);
}
