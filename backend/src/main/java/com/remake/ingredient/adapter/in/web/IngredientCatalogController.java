package com.remake.ingredient.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.remake.global.config.OpenApiConfig;
import com.remake.ingredient.application.port.in.GetIngredientCatalogUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 재료 사전 검색. 재료 추가의 이름 자동완성과 온보딩·설정의 제외 재료 검색이 쓴다. */
@Tag(name = "재료 사전")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequestMapping("/api/v1/user/ingredient-catalog")
@RequiredArgsConstructor
public class IngredientCatalogController {

	private final GetIngredientCatalogUseCase getIngredientCatalogUseCase;

	@Operation(summary = "재료 사전 검색",
			description = "q는 이름에 포함된 항목(선택, 50자 이하), 최대 50개. commonExclusion=true면 대표 알레르기 재료를 맨 위에 둡니다.")
	@GetMapping
	public List<IngredientCatalogResponse> search(@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "false") boolean commonExclusion) {
		return getIngredientCatalogUseCase.searchCatalog(q, commonExclusion).stream()
				.map(IngredientCatalogResponse::from)
				.toList();
	}
}
