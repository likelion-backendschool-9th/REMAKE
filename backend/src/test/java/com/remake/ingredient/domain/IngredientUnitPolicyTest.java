package com.remake.ingredient.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

// 단위 결정 규칙 중 후보 고르기: Spring 없이 확인한다.
class IngredientUnitPolicyTest {

	private static IngredientCatalogItem item(long id, String name, IngredientUnit unit) {
		return new IngredientCatalogItem(id, 1L, name, unit, null, false);
	}

	@Test
	void 기본_단위는_EA다() {
		assertThat(IngredientUnitPolicy.DEFAULT_UNIT).isEqualTo(IngredientUnit.EA);
	}

	@Test
	void 재료명_안에_들어_있는_사전_이름을_찾는다() {
		List<IngredientCatalogItem> items = List.of(item(1, "돼지고기", IngredientUnit.G), item(2, "소고기", IngredientUnit.G));

		assertThat(IngredientUnitPolicy.findContainedItem("한돈 앞다리 돼지고기", items))
				.map(IngredientCatalogItem::name).contains("돼지고기");
		assertThat(IngredientUnitPolicy.findContainedItem("닭가슴살", items)).isEmpty();
	}

	@Test
	void 여러_개가_걸리면_가장_긴_이름을_쓴다() {
		List<IngredientCatalogItem> items = List.of(item(1, "두부", IngredientUnit.EA), item(2, "순두부", IngredientUnit.PACK));

		assertThat(IngredientUnitPolicy.findContainedItem("부드러운 순두부", items))
				.map(IngredientCatalogItem::unit).contains(IngredientUnit.PACK);
	}

	@Test
	void 길이가_같으면_id가_작은_항목을_쓴다() {
		List<IngredientCatalogItem> items = List.of(item(7, "우유", IngredientUnit.ML), item(3, "치즈", IngredientUnit.PACK));

		assertThat(IngredientUnitPolicy.findContainedItem("우유치즈", items))
				.map(IngredientCatalogItem::name).contains("치즈");
	}

	@Test
	void 한_글자_사전_이름은_재료명_안에서_찾지_않는다() {
		List<IngredientCatalogItem> items = List.of(item(1, "김", IngredientUnit.SHEET), item(2, "무", IngredientUnit.EA));

		assertThat(IngredientUnitPolicy.findContainedItem("구운김", items)).isEmpty();
		assertThat(IngredientUnitPolicy.findContainedItem("무말랭이", items)).isEmpty();
	}

	@Test
	void 입력이나_후보가_없으면_빈_값이다() {
		assertThat(IngredientUnitPolicy.findContainedItem(null, List.of(item(1, "두부", IngredientUnit.EA)))).isEmpty();
		assertThat(IngredientUnitPolicy.findContainedItem("두부", null)).isEmpty();
		assertThat(IngredientUnitPolicy.findContainedItem("두부", List.of())).isEmpty();
	}
}
