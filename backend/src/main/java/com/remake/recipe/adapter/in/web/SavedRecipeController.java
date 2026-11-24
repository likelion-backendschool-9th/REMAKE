package com.remake.recipe.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.remake.global.config.OpenApiConfig;
import com.remake.global.web.LoginUserId;
import com.remake.recipe.application.port.in.GetSavedRecipesUseCase;
import com.remake.recipe.application.port.in.SaveRecipeUseCase;
import com.remake.recipe.domain.SavedRecipeFilter;
import com.remake.recipe.domain.SavedRecipeSort;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/** 저장한 레시피(API 33~37). 사용자는 항상 로그인한 사용자 본인이다. */
@Tag(name = "레시피")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequestMapping("/api/v1/user/saved-recipes")
@RequiredArgsConstructor
public class SavedRecipeController {

	private final GetSavedRecipesUseCase getSavedRecipesUseCase;
	private final SaveRecipeUseCase saveRecipeUseCase;

	@Operation(summary = "저장 목록", description = """
			totalCount는 필터와 상관없는 전체 저장 수입니다. 바로 가능(AVAILABLE)은 물을 뺀 모든 재료가 내 냉장고에 같은 이름으로 있는 레시피입니다
			(소비기한 지난 재료는 없는 것으로 봄, 양은 아직 비교하지 않음). 재료 요약은 앞 3개를 " · "로 잇고 나머지는 extraIngredientCount입니다.""")
	@GetMapping
	public SavedRecipesResponse list(@LoginUserId Long userId,
			@RequestParam(defaultValue = "ALL") SavedRecipeFilter filter,
			@RequestParam(defaultValue = "SAVED_DESC") SavedRecipeSort sort,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return SavedRecipesResponse.from(getSavedRecipesUseCase.getSavedRecipes(userId, filter, sort, page, size));
	}

	@Operation(summary = "레시피 저장", description = """
			내가 만든 레시피를 저장합니다. 이미 저장돼 있어도 같은 결과입니다. 재료 보충으로 들어간 부족 재료는 장바구니에 담습니다
			(장바구니 기능이 생기기 전까지 cartAddedCount는 0).""")
	@PutMapping("/{recipeId}")
	public SaveRecipeResponse save(@LoginUserId Long userId, @PathVariable Long recipeId) {
		SaveRecipeUseCase.SaveResult result = saveRecipeUseCase.save(userId, recipeId);
		return new SaveRecipeResponse(result.saved(), result.cartAddedCount());
	}

	@Operation(summary = "저장 취소", description = "저장 목록에서만 뺍니다. 레시피·공유 글·장바구니는 그대로입니다.")
	@DeleteMapping("/{recipeId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unsave(@LoginUserId Long userId, @PathVariable Long recipeId) {
		saveRecipeUseCase.unsave(userId, recipeId);
	}

	@Operation(summary = "즐겨찾기 켜기", description = "저장한 레시피만 됩니다. 아니면 SAVED_RECIPE_NOT_FOUND(404)입니다.")
	@PutMapping("/{recipeId}/favorite")
	public FavoriteResponse favorite(@LoginUserId Long userId, @PathVariable Long recipeId) {
		return new FavoriteResponse(saveRecipeUseCase.setFavorite(userId, recipeId, true));
	}

	@Operation(summary = "즐겨찾기 끄기", description = "저장은 그대로 둡니다.")
	@DeleteMapping("/{recipeId}/favorite")
	public FavoriteResponse unfavorite(@LoginUserId Long userId, @PathVariable Long recipeId) {
		return new FavoriteResponse(saveRecipeUseCase.setFavorite(userId, recipeId, false));
	}
}
