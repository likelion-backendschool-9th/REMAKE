package com.remake.ingredient.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.remake.global.config.OpenApiConfig;
import com.remake.ingredient.application.port.in.ResolveIngredientUnitUseCase;

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

	// 정의서대로 이름만 받는다. 카테고리가 없어 사전 이름과 정확히 같을 때만 사전 단위이고 아니면 EA이다.
	// 재료를 저장할 때는 카테고리까지 써서 단위를 정하므로 이 값과 다를 수 있다. 저장 응답의 unit이 최종 값이다.
	@Operation(summary = "재료명에 맞는 단위", description = "재료명으로 단위를 미리 알려 줍니다. 사전 이름과 정확히 같으면 그 단위, 아니면 EA입니다. "
			+ "저장 응답의 unit이 최종 값이며 미리 본 값과 다를 수 있습니다(수식어가 붙은 이름 등).")
	@GetMapping
	public IngredientUnitResponse getUnit(@RequestParam(required = false) String name) {
		return new IngredientUnitResponse(resolveIngredientUnitUseCase.resolveUnit(name, null));
	}
}
