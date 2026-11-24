package com.remake.ingredient.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.remake.global.config.OpenApiConfig;
import com.remake.ingredient.application.port.in.ResolveIngredientUnitUseCase;
import com.remake.ingredient.domain.IngredientCategoryCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 재료명에 맞는 단위를 서버가 정해 알려 주는 API 14. 단위를 저장하지 않고 조회만 한다. */
@Tag(name = "재료 단위")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequestMapping("/api/v1/user/ingredient-units")
@RequiredArgsConstructor
public class IngredientUnitController {

	private final ResolveIngredientUnitUseCase resolveIngredientUnitUseCase;

	// 이름과 카테고리로 저장(15·16·21)과 같은 규칙을 써서, 미리 본 단위와 저장되는 단위가 같게 한다.
	// 카테고리는 선택이다. 없으면 사전 이름과 정확히 같을 때만 사전 단위이고 아니면 EA이다.
	@Operation(summary = "재료명에 맞는 단위", description = "재료명(필수)과 카테고리(선택)로 단위를 알려 줍니다. 재료 저장과 같은 규칙입니다. "
			+ "카테고리를 넣으면 저장될 단위와 같고, 없으면 사전 이름과 정확히 같을 때만 사전 단위, 아니면 EA입니다. "
			+ "이름이나 카테고리가 바뀌면 다시 부릅니다.")
	@GetMapping
	public IngredientUnitResponse getUnit(@RequestParam(required = false) String name,
			@RequestParam(required = false) IngredientCategoryCode categoryCode) {
		return new IngredientUnitResponse(resolveIngredientUnitUseCase.resolveUnit(name, categoryCode));
	}
}
