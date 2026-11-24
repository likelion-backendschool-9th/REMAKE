package com.remake.user.adapter.in.web;

import java.util.ArrayList;
import java.util.List;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.user.application.port.in.CompleteOnboardingUseCase;
import com.remake.user.domain.AllergenCode;
import com.remake.user.domain.User;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 온보딩 저장 요청. 가구원 수는 필수(1~50), 알레르기는 0개여도 된다.
 */
public record OnboardingRequest(
		@NotNull @Min(1) @Max(User.MAX_HOUSEHOLD_SIZE) Integer householdSize,
		List<String> allergenCodes) {

	CompleteOnboardingUseCase.Command toCommand(Long userId) {
		return new CompleteOnboardingUseCase.Command(userId, householdSize, parseAllergenCodes(allergenCodes));
	}

	static List<AllergenCode> parseAllergenCodes(List<String> raw) {
		if (raw == null || raw.isEmpty()) {
			return List.of();
		}
		List<AllergenCode> codes = new ArrayList<>(raw.size());
		for (String value : raw) {
			AllergenCode code = AllergenCode.fromWire(value);
			if (code == null) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "알 수 없는 알레르기 코드입니다: " + value);
			}
			codes.add(code);
		}
		return codes;
	}
}
