package com.remake.user.application.port.in;

import java.util.List;

import com.remake.user.domain.AllergenCode;

public interface CompleteOnboardingUseCase {

	record Command(Long userId, int householdSize, List<AllergenCode> allergenCodes) {
	}

	void complete(Command command);
}
