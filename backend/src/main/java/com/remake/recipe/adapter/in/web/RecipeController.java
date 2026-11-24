package com.remake.recipe.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.remake.global.config.OpenApiConfig;
import com.remake.global.web.LoginUserId;
import com.remake.recipe.application.port.in.GenerateRecipeUseCase;
import com.remake.recipe.application.port.in.GetRecipeUseCase;
import com.remake.recipe.application.port.in.RegenerateRecipeUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** AI 레시피. 사용자는 항상 로그인한 사용자 본인이다. */
@Tag(name = "레시피")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequestMapping("/api/v1/user/recipes")
@RequiredArgsConstructor
public class RecipeController {

	private final GenerateRecipeUseCase generateRecipeUseCase;
	private final RegenerateRecipeUseCase regenerateRecipeUseCase;
	private final GetRecipeUseCase getRecipeUseCase;

	@Operation(summary = "레시피 생성", description = """
			빠른 생성은 냉장고 재료와 가구원 수, 커스텀은 고른 옵션으로 만듭니다. 한 번에 응답하고(최대 60초) 성공할 때만 하루 횟수를 차감합니다.
			재료 보충이 꺼져 있으면 내 재료로만 만들고, 양념을 뺀 재료가 2개 미만이면 NO_INGREDIENTS(422)입니다.
			AI 오류·시간 초과·형식 오류는 한 번 다시 시도한 뒤 실패합니다.""")
	@PostMapping("/generate")
	@ResponseStatus(HttpStatus.CREATED)
	public RecipeDetailResponse generate(@LoginUserId Long userId, @Valid @RequestBody GenerateRecipeRequest request) {
		return RecipeDetailResponse.from(generateRecipeUseCase.generate(userId, request.toCommand()));
	}

	@Operation(summary = "레시피 다시 받기", description = """
			같은 조건으로 새 레시피를 만듭니다. 언제든 누를 수 있고 하루 횟수를 1회 씁니다(성공할 때만). 원래 레시피는 그대로 남습니다.
			커스텀에서 고른 재료 중 그 사이 없어진 재료는 빼고 만듭니다.""")
	@PostMapping("/{id}/regenerate")
	@ResponseStatus(HttpStatus.CREATED)
	public RecipeDetailResponse regenerate(@LoginUserId Long userId, @PathVariable Long id) {
		return RecipeDetailResponse.from(regenerateRecipeUseCase.regenerate(userId, id));
	}

	@Operation(summary = "레시피 상세", description = "내가 만들었거나 저장한 레시피(저장한 공유 레시피 포함)만 볼 수 있습니다. 그 밖은 RECIPE_NOT_FOUND(404)입니다.")
	@GetMapping("/{id}")
	public RecipeDetailResponse detail(@LoginUserId Long userId, @PathVariable Long id) {
		return RecipeDetailResponse.from(getRecipeUseCase.getRecipe(userId, id));
	}
}
