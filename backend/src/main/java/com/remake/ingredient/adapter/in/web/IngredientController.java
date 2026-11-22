package com.remake.ingredient.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.remake.global.config.OpenApiConfig;
import com.remake.global.web.LoginUserId;
import com.remake.ingredient.application.port.in.DeleteIngredientUseCase;
import com.remake.ingredient.application.port.in.GetFridgeUseCase;
import com.remake.ingredient.application.port.in.GetIngredientCategoriesUseCase;
import com.remake.ingredient.application.port.in.GetIngredientUseCase;
import com.remake.ingredient.application.port.in.RegisterIngredientUseCase;
import com.remake.ingredient.application.port.in.UpdateIngredientUseCase;
import com.remake.ingredient.domain.ExpiryStatus;
import com.remake.ingredient.domain.Ingredient;
import com.remake.ingredient.domain.IngredientCategory;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.StorageLocation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 냉장고 재료 등록·상세·수정·삭제. 사용자는 항상 로그인한 사용자 본인이고, 다른 사용자의 재료는 404이다. */
@Tag(name = "재료")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequestMapping("/api/v1/user/ingredients")
@RequiredArgsConstructor
public class IngredientController {

	private final RegisterIngredientUseCase registerIngredientUseCase;
	private final GetIngredientUseCase getIngredientUseCase;
	private final UpdateIngredientUseCase updateIngredientUseCase;
	private final DeleteIngredientUseCase deleteIngredientUseCase;
	private final GetIngredientCategoriesUseCase getIngredientCategoriesUseCase;
	private final GetFridgeUseCase getFridgeUseCase;

	@Operation(summary = "냉장고 목록", description = "진행 중인 재료만, 소비기한이 빠른 순입니다. status·storage·categoryCode는 모두 선택이고 없으면 전체입니다. 페이징은 없습니다.")
	@GetMapping
	public FridgeIngredientListResponse list(@LoginUserId Long userId,
			@RequestParam(required = false) ExpiryStatus status,
			@RequestParam(required = false) StorageLocation storage,
			@RequestParam(required = false) IngredientCategoryCode categoryCode) {
		return new FridgeIngredientListResponse(getFridgeUseCase.getFridgeIngredients(userId, status, storage,
				categoryCode).stream()
				.map(FridgeIngredientResponse::from)
				.toList());
	}

	@Operation(summary = "냉장고 요약", description = "진행 중인 재료의 total·danger·soon·ok. 완료된 재료는 제외합니다.")
	@GetMapping("/summary")
	public FridgeSummaryResponse summary(@LoginUserId Long userId) {
		return FridgeSummaryResponse.from(getFridgeUseCase.getFridgeSummary(userId));
	}

	@Operation(summary = "재료 직접 추가", description = "냉장고 단위 9개 코드만 허용합니다(아니면 INVALID_UNIT).")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public IngredientResponse register(@LoginUserId Long userId, @Valid @RequestBody IngredientCreateRequest request) {
		Long id = registerIngredientUseCase.registerIngredient(request.toCommand(userId));
		return toResponse(getIngredientUseCase.getIngredient(userId, id));
	}

	@Operation(summary = "재료 상세")
	@GetMapping("/{id}")
	public IngredientResponse get(@LoginUserId Long userId, @PathVariable Long id) {
		return toResponse(getIngredientUseCase.getIngredient(userId, id));
	}

	@Operation(summary = "재료 수정", description = "이름·카테고리·보관 위치·소비기한·가격. 소비기한을 고치면 추정 표시가 꺼집니다. 가격은 null을 보내면 지웁니다.")
	@PatchMapping("/{id}")
	public IngredientResponse update(@LoginUserId Long userId, @PathVariable Long id,
			@Valid @RequestBody IngredientUpdateRequest request) {
		return toResponse(updateIngredientUseCase.updateIngredient(request.toCommand(userId, id)));
	}

	@Operation(summary = "재료 삭제")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@LoginUserId Long userId, @PathVariable Long id) {
		deleteIngredientUseCase.deleteIngredient(userId, id);
	}

	// 응답에는 카테고리 id가 아니라 code를 준다. 카테고리는 12개라 목록에서 찾는다.
	private IngredientResponse toResponse(Ingredient ingredient) {
		IngredientCategoryCode code = getIngredientCategoriesUseCase.getIngredientCategories().stream()
				.filter(category -> category.id().equals(ingredient.categoryId()))
				.map(IngredientCategory::code)
				.findFirst()
				.orElseThrow();
		return IngredientResponse.from(ingredient, code);
	}
}
