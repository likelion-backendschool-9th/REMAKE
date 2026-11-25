package com.remake.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.recipe.application.port.in.GetRecipeIngredientSummariesUseCase;
import com.remake.recipe.application.port.out.SaveRecipePort;
import com.remake.recipe.domain.IngredientSummary;
import com.remake.recipe.domain.Recipe;
import com.remake.recipe.domain.RecipeIngredient;
import com.remake.recipe.domain.RecipeSource;
import com.remake.recipe.domain.RecipeUnit;
import com.remake.support.IntegrationTestSupport;

// 공유 목록·랭킹(API 49·50)용 재료 요약 일괄 읽기
class RecipeIngredientSummariesTest extends IntegrationTestSupport {

	@Autowired
	private SaveRecipePort saveRecipePort;
	@Autowired
	private GetRecipeIngredientSummariesUseCase getRecipeIngredientSummariesUseCase;

	private Long recipe(Long owner, String... ingredientNames) {
		List<RecipeIngredient> ingredients = Arrays.stream(ingredientNames)
				.map(name -> new RecipeIngredient(null, name, BigDecimal.ONE, RecipeUnit.EA))
				.toList();
		return transactionTemplate.execute(status -> saveRecipePort.save(Recipe.create(owner, RecipeSource.QUICK,
				"레시피", 1, 10, null, null, ingredients, List.of("만든다.")))).id();
	}

	@Test
	void 여러_사람의_레시피_재료_요약을_한_번에_돌려주고_없는_id는_뺀다() {
		Long five = recipe(createUser(), "닭가슴살", "청상추", "계란", "물", "소금");
		Long two = recipe(createUser(), "양파", "버섯");

		Map<Long, IngredientSummary> summaries = getRecipeIngredientSummariesUseCase
				.getIngredientSummaries(List.of(two, five, 999999L));

		assertThat(summaries).containsOnlyKeys(five, two);
		// 재료 순서대로 앞 3개, 나머지는 개수만(물·소금도 셈)
		assertThat(summaries.get(five)).isEqualTo(new IngredientSummary("닭가슴살 · 청상추 · 계란", 2));
		assertThat(summaries.get(two)).isEqualTo(new IngredientSummary("양파 · 버섯", 0));
	}

	@Test
	void 빈_목록이면_빈_결과다() {
		assertThat(getRecipeIngredientSummariesUseCase.getIngredientSummaries(List.of())).isEmpty();
	}
}
