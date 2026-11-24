package com.remake.user.application.port.out;

import java.util.List;

import com.remake.user.domain.AllergenCode;

public interface LoadExcludedIngredientPort {

	List<AllergenCode> findAllergenCodesByUserId(Long userId);

	int countByUserId(Long userId);
}
