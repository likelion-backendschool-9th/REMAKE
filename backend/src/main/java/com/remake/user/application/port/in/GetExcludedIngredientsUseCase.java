package com.remake.user.application.port.in;

import java.util.List;

import com.remake.user.domain.AllergenCode;

public interface GetExcludedIngredientsUseCase {

	List<AllergenCode> getAllergenCodes(Long userId);
}
