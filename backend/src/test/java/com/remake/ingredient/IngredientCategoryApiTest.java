package com.remake.ingredient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.ingredient.application.port.in.GetIngredientCategoriesUseCase;
import com.remake.ingredient.domain.IngredientCategory;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.StorageLocation;
import com.remake.support.IntegrationTestSupport;

// 예시 API: Controller → UseCase → Service → Port → 저장 어댑터 → DB(Flyway 기준 데이터)까지 한 번에 확인한다.
// 기준 데이터는 id를 하드코딩하지 않고 code로 찾는다(backend-testing.md 통합 테스트 규칙 4번).
class IngredientCategoryApiTest extends IntegrationTestSupport {

	@Autowired
	private GetIngredientCategoriesUseCase getIngredientCategoriesUseCase;

	@Test
	void 카테고리_목록을_감싸지_않고_배열로_돌려준다() throws Exception {
		String[] codes = Arrays.stream(IngredientCategoryCode.values()).map(Enum::name).toArray(String[]::new);

		mockMvc.perform(get("/api/v1/user/ingredient-categories")
						.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, 1L))
				.andExpect(status().isOk())
				// ApiResponse 같은 감싸기 없이 최상위가 배열이고, DB의 code 12개가 enum과 같은 순서로 나온다
				.andExpect(jsonPath("$[*].code", contains(codes)))
				// enum은 이름 그대로 나간다
				.andExpect(jsonPath("$[?(@.code == 'VEGETABLE')].name", contains("채소")))
				.andExpect(jsonPath("$[?(@.code == 'VEGETABLE')].defaultStorage", contains("FRIDGE")))
				.andExpect(jsonPath("$[?(@.code == 'VEGETABLE')].defaultShelfLifeDays", contains(7)));
	}

	@Test
	void code로_카테고리_하나를_읽는다() {
		IngredientCategory category = getIngredientCategoriesUseCase
				.getIngredientCategoryByCode(IngredientCategoryCode.SEASONING);

		assertThat(category.id()).isEqualTo(categoryId("SEASONING"));
		assertThat(category.name()).isEqualTo("양념·소스");
		assertThat(category.defaultStorage()).isEqualTo(StorageLocation.PANTRY);
	}

	@Test
	void id로_카테고리_하나를_읽는다() {
		IngredientCategory category = getIngredientCategoriesUseCase.getIngredientCategory(categoryId("MEAT"));

		assertThat(category.code()).isEqualTo(IngredientCategoryCode.MEAT);
	}

	@Test
	void 없는_카테고리는_BusinessException을_던진다() {
		Long missingId = jdbcTemplate.queryForObject("SELECT MAX(id) + 1 FROM ingredient_categories", Long.class);

		assertThatThrownBy(() -> getIngredientCategoriesUseCase.getIngredientCategory(missingId))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INGREDIENT_CATEGORY_NOT_FOUND);
	}
}
