package com.remake.mealplan.adapter.out.mfds;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.mealplan.application.port.out.LoadMealPlanCombinationRulesPort;
import com.remake.support.IntegrationTestSupport;

/**
 * 조합 검사 JSON의 재료 사전 사본이 DB 재료 사전(마이그레이션 전부 적용 뒤)과 같은지 본다(#288).
 * 사전을 바꾸는 마이그레이션이 생기면 여기서 깨진다: scripts/mfds/build_combination_check.py를 다시 돌린다.
 */
class MealPlanCombinationCatalogSyncTest extends IntegrationTestSupport {

	@Autowired
	private LoadMealPlanCombinationRulesPort loadMealPlanCombinationRulesPort;

	@Test
	void 조합_검사_파일의_사전_사본이_DB_재료_사전과_같다() {
		Map<String, IngredientCategoryCode> database = new LinkedHashMap<>();
		jdbcTemplate.query("""
				SELECT c.name, k.code
				FROM ingredient_catalog c
				JOIN ingredient_categories k ON k.id = c.category_id
				ORDER BY c.id
				""", row -> {
			database.put(row.getString("name"), IngredientCategoryCode.valueOf(row.getString("code")));
		});

		assertThat(database).isNotEmpty();
		assertThat(loadMealPlanCombinationRulesPort.load().catalog())
				.as("재료 사전이 바뀌었으면 scripts/mfds/build_combination_check.py를 다시 돌린다")
				.isEqualTo(database);
	}
}
