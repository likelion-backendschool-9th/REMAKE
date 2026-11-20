package com.remake.ingredient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.GetIngredientCategoriesUseCase;
import com.remake.ingredient.domain.IngredientCategory;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.Storage;
import com.remake.support.IntegrationTestSupport;

// 예시 API: Controller → UseCase → Service → Port → 저장 어댑터 → DB(Flyway 기준 데이터)까지 한 번에 확인한다.
class IngredientCategoryApiTest extends IntegrationTestSupport {

	@Autowired
	private GetIngredientCategoriesUseCase getIngredientCategoriesUseCase;

	@Test
	void 카테고리_목록을_감싸지_않고_배열로_돌려준다() throws Exception {
		mockMvc.perform(get("/api/v1/user/ingredient-categories"))
				.andExpect(status().isOk())
				// ApiResponse 같은 감싸기 없이 최상위가 배열이다
				.andExpect(jsonPath("$.length()").value(12))
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].code").value("VEGETABLE"))
				.andExpect(jsonPath("$[0].name").value("채소"))
				.andExpect(jsonPath("$[0].imageUrl").value("/images/categories/vegetable.png"))
				// enum은 이름 그대로 나간다
				.andExpect(jsonPath("$[0].defaultStorage").value("FRIDGE"))
				.andExpect(jsonPath("$[0].defaultUnit").value("G"))
				.andExpect(jsonPath("$[0].defaultShelfLifeDays").value(7))
				.andExpect(jsonPath("$[11].code").value("ETC"));
	}

	@Test
	void 카테고리_하나를_읽는다() {
		IngredientCategory category = getIngredientCategoriesUseCase.getIngredientCategory(10L);

		assertThat(category.code()).isEqualTo("SEASONING");
		assertThat(category.defaultStorage()).isEqualTo(Storage.PANTRY);
		assertThat(category.defaultUnit()).isEqualTo(IngredientUnit.ML);
	}

	@Test
	void 없는_카테고리는_BusinessException을_던진다() {
		assertThatThrownBy(() -> getIngredientCategoriesUseCase.getIngredientCategory(999L))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INGREDIENT_CATEGORY_NOT_FOUND);
	}
}
