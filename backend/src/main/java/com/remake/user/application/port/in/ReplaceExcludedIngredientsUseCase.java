package com.remake.user.application.port.in;

import java.util.List;

import com.remake.user.domain.AllergenCode;

public interface ReplaceExcludedIngredientsUseCase {

	List<AllergenCode> replaceAllergenCodes(Long userId, List<AllergenCode> allergenCodes);
}
