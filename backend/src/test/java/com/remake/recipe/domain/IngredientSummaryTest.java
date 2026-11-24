package com.remake.recipe.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class IngredientSummaryTest {

	@Test
	void 앞_3개를_가운뎃점으로_잇고_나머지는_개수로_준다() {
		assertThat(IngredientSummary.of(List.of("닭가슴살", "청상추", "계란", "토마토", "소금", "물")))
				.isEqualTo(new IngredientSummary("닭가슴살 · 청상추 · 계란", 3));
	}

	@Test
	void 세_개_이하면_모두_보이고_나머지는_0이다() {
		assertThat(IngredientSummary.of(List.of("계란", "양파", "대파"))).isEqualTo(new IngredientSummary("계란 · 양파 · 대파", 0));
		assertThat(IngredientSummary.of(List.of("계란"))).isEqualTo(new IngredientSummary("계란", 0));
		assertThat(IngredientSummary.of(List.of())).isEqualTo(new IngredientSummary("", 0));
	}
}
