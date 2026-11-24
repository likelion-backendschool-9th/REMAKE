package com.remake.user.adapter.in.web;

import java.util.List;

import com.remake.user.domain.AllergenCode;

public record ExcludedIngredientsResponse(List<AllergenCode> allergenCodes) {

	static ExcludedIngredientsResponse from(List<AllergenCode> codes) {
		return new ExcludedIngredientsResponse(codes);
	}
}
