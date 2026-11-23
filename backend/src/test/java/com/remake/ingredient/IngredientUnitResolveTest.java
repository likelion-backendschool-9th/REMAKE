package com.remake.ingredient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.ResolveIngredientUnitUseCase;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.support.IntegrationTestSupport;

// 재료명으로 단위 정하기(#145): 실제 사전(V5)으로 정확히 같음 → 들어 있는 이름(같은 카테고리) → EA 순서를 확인한다.
class IngredientUnitResolveTest extends IntegrationTestSupport {

	@Autowired
	private ResolveIngredientUnitUseCase resolveIngredientUnitUseCase;

	private IngredientUnit resolve(String name, IngredientCategoryCode category) {
		return resolveIngredientUnitUseCase.resolveUnit(name, category);
	}

	@Test
	void 사전_이름과_정확히_같으면_사전_단위다() {
		assertThat(resolve("우유", IngredientCategoryCode.DAIRY_EGG)).isEqualTo(IngredientUnit.PACK);
		assertThat(resolve("대파", IngredientCategoryCode.VEGETABLE)).isEqualTo(IngredientUnit.BUNCH);
		assertThat(resolve("  쌀  ", IngredientCategoryCode.GRAIN)).isEqualTo(IngredientUnit.KG);
	}

	@Test
	void 정확히_같으면_입력한_카테고리가_달라도_사전_단위다() {
		assertThat(resolve("우유", IngredientCategoryCode.ETC)).isEqualTo(IngredientUnit.PACK);
		assertThat(resolve("우유", null)).isEqualTo(IngredientUnit.PACK);
	}

	@Test
	void 수식어가_붙은_이름은_같은_카테고리의_사전_이름으로_정한다() {
		assertThat(resolve("한돈 앞다리 돼지고기", IngredientCategoryCode.MEAT)).isEqualTo(IngredientUnit.G);
		assertThat(resolve("유기농 우유", IngredientCategoryCode.DAIRY_EGG)).isEqualTo(IngredientUnit.PACK);
		assertThat(resolve("맛있는 김치", IngredientCategoryCode.SIDE_DISH)).isEqualTo(IngredientUnit.G);
	}

	@Test
	void 카테고리가_다르면_들어_있어도_EA다() {
		// 쌀(GRAIN, KG)이 들어 있지만 가공식품이라 인정하지 않는다
		assertThat(resolve("쌀과자", IngredientCategoryCode.PROCESSED)).isEqualTo(IngredientUnit.EA);
	}

	@Test
	void 한_글자_사전_이름은_정확히_같을_때만_인정한다() {
		assertThat(resolve("김", IngredientCategoryCode.SEAFOOD)).isEqualTo(IngredientUnit.SHEET);
		assertThat(resolve("구운김", IngredientCategoryCode.SEAFOOD)).isEqualTo(IngredientUnit.EA);
		// 쌀(KG)도 한 글자라 햅쌀에서는 찾지 않는다
		assertThat(resolve("햅쌀", IngredientCategoryCode.GRAIN)).isEqualTo(IngredientUnit.EA);
	}

	@Test
	void 사전에_없으면_EA다() {
		assertThat(resolve("없는재료이름", IngredientCategoryCode.VEGETABLE)).isEqualTo(IngredientUnit.EA);
		assertThat(resolve("없는재료이름", null)).isEqualTo(IngredientUnit.EA);
	}

	@Test
	void 카테고리가_없으면_정확히_같은_이름만_찾는다() {
		assertThat(resolve("유기농 우유", null)).isEqualTo(IngredientUnit.EA);
	}

	@Test
	void 이름이_비어_있으면_INVALID_INPUT이다() {
		assertThatThrownBy(() -> resolve("  ", IngredientCategoryCode.MEAT))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
		assertThatThrownBy(() -> resolve(null, null)).isInstanceOf(BusinessException.class);
	}
}
