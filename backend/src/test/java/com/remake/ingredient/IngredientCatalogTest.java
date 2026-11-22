package com.remake.ingredient;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.ingredient.application.port.in.GetIngredientCatalogUseCase;
import com.remake.ingredient.domain.IngredientCatalogItem;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.support.IntegrationTestSupport;

// 재료 사전: Flyway 기준 데이터(V5)와 다른 기능이 쓰는 UseCase를 확인한다. 사전은 서버 안에서만 쓰고 공개 API가 없다.
class IngredientCatalogTest extends IntegrationTestSupport {

	@Autowired
	private GetIngredientCatalogUseCase getIngredientCatalogUseCase;

	@Test
	void 기준_데이터는_카테고리_12종_모두에_항목이_있고_이름이_겹치지_않는다() {
		Integer categories = jdbcTemplate.queryForObject("SELECT count(DISTINCT category_id) FROM ingredient_catalog",
				Integer.class);
		Integer total = jdbcTemplate.queryForObject("SELECT count(*) FROM ingredient_catalog", Integer.class);
		Integer distinct = jdbcTemplate.queryForObject("SELECT count(DISTINCT name) FROM ingredient_catalog",
				Integer.class);

		assertThat(categories).isEqualTo(12);
		assertThat(total).isEqualTo(distinct).isGreaterThan(100);
	}

	@Test
	void 대표_알레르기_재료가_들어_있다() {
		List<String> common = jdbcTemplate.queryForList(
				"SELECT name FROM ingredient_catalog WHERE is_common_exclusion ORDER BY name", String.class);

		assertThat(common).contains("달걀", "우유", "땅콩", "대두", "밀", "메밀", "갑각류", "새우", "게", "고등어", "돼지고기",
				"복숭아", "토마토", "호두");
	}

	@Test
	void 같은_재료의_다른_표기는_각자_행이_있고_대표_알레르기는_하나에만_표시한다() {
		List<String> common = jdbcTemplate.queryForList(
				"SELECT name FROM ingredient_catalog WHERE is_common_exclusion", String.class);

		assertThat(common).contains("달걀", "쇠고기").doesNotContain("계란", "소고기");
		assertThat(getIngredientCatalogUseCase.findCatalogItemByName("계란")).isPresent();
		assertThat(getIngredientCatalogUseCase.findCatalogItemByName("소고기")).isPresent();
	}

	@Test
	void 모든_항목에_냉장고_단위가_있고_대표_재료는_흔히_사는_단위다() {
		Integer withoutUnit = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM ingredient_catalog WHERE unit NOT IN ('G', 'KG', 'ML', 'L', 'EA', 'PACK', 'BUNCH', 'PIECE', 'SHEET')",
				Integer.class);

		assertThat(withoutUnit).isZero();
		assertThat(unitOf("달걀")).isEqualTo(IngredientUnit.EA);
		assertThat(unitOf("계란")).isEqualTo(IngredientUnit.EA);
		assertThat(unitOf("우유")).isEqualTo(IngredientUnit.PACK);
		assertThat(unitOf("두부")).isEqualTo(IngredientUnit.EA);
		assertThat(unitOf("대파")).isEqualTo(IngredientUnit.BUNCH);
		assertThat(unitOf("쌀")).isEqualTo(IngredientUnit.KG);
		assertThat(unitOf("간장")).isEqualTo(IngredientUnit.ML);
		assertThat(unitOf("돼지고기")).isEqualTo(IngredientUnit.G);
	}

	private IngredientUnit unitOf(String name) {
		return getIngredientCatalogUseCase.findCatalogItemByName(name).orElseThrow().unit();
	}

	@Test
	void 다른_기능이_쓰는_UseCase는_id와_이름으로_읽는다() {
		IngredientCatalogItem egg = getIngredientCatalogUseCase.findCatalogItemByName(" 달걀 ").orElseThrow();

		assertThat(egg.commonExclusion()).isTrue();
		assertThat(getIngredientCatalogUseCase.getCatalogItemsByIds(List.of(egg.id(), -1L)))
				.extracting(IngredientCatalogItem::name).containsExactly("달걀");
		assertThat(getIngredientCatalogUseCase.getCatalogItemsByIds(List.of())).isEmpty();
		assertThat(getIngredientCatalogUseCase.findCatalogItemByName("없는재료이름")).isEmpty();
		assertThat(getIngredientCatalogUseCase.findCatalogItemByName("  ")).isEmpty();
	}
}
