package com.remake.user.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.remake.global.config.OpenApiConfig;
import com.remake.global.web.LoginUserId;
import com.remake.user.application.port.in.GetExcludedIngredientsUseCase;
import com.remake.user.application.port.in.ReplaceExcludedIngredientsUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "제외 재료")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequestMapping("/api/v1/user/excluded-ingredients")
@RequiredArgsConstructor
public class ExcludedIngredientController {

	private final GetExcludedIngredientsUseCase getExcludedIngredientsUseCase;
	private final ReplaceExcludedIngredientsUseCase replaceExcludedIngredientsUseCase;

	@Operation(summary = "제외 재료 조회")
	@GetMapping
	public ExcludedIngredientsResponse get(@LoginUserId Long userId) {
		return ExcludedIngredientsResponse.from(getExcludedIngredientsUseCase.getAllergenCodes(userId));
	}

	@Operation(summary = "제외 재료 교체", description = "칩 코드를 통째로 바꾼다. 0개여도 저장된다.")
	@PutMapping
	public ExcludedIngredientsResponse replace(
			@LoginUserId Long userId,
			@Valid @RequestBody ReplaceExcludedIngredientsRequest request) {
		return ExcludedIngredientsResponse.from(replaceExcludedIngredientsUseCase.replaceAllergenCodes(
				userId, OnboardingRequest.parseAllergenCodes(request.allergenCodes())));
	}
}
