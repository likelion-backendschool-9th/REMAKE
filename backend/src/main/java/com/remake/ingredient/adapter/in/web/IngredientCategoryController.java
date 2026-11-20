package com.remake.ingredient.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.remake.global.config.OpenApiConfig;
import com.remake.ingredient.application.port.in.GetIngredientCategoriesUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Controller는 UseCase 인터페이스만 부르고, 결과를 Response DTO로 바꿔 그대로 돌려준다(200).
 * 경로는 /api/v1/{public|user|admin}/{복수형 리소스}를 Controller에 직접 쓴다.
 */
@Tag(name = "재료 카테고리")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/user/ingredient-categories")
@RequiredArgsConstructor
public class IngredientCategoryController {

	private final GetIngredientCategoriesUseCase getIngredientCategoriesUseCase;

	// 개수가 정해진 기준 데이터(12개)라 페이지로 나누지 않고 배열로 준다. 계속 늘어나는 목록은 PageResponse를 쓴다.
	@Operation(summary = "재료 카테고리 목록", description = "재료 추가 화면의 재료 종류와 기본값(보관 위치, 단위, 소비기한 일수)")
	@GetMapping
	public List<IngredientCategoryResponse> getIngredientCategories() {
		return getIngredientCategoriesUseCase.getIngredientCategories().stream()
				.map(IngredientCategoryResponse::from)
				.toList();
	}
}
