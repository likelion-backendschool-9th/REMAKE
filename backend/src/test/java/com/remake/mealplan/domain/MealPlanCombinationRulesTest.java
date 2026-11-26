package com.remake.mealplan.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.remake.ingredient.domain.IngredientCategoryCode;

class MealPlanCombinationRulesTest {

	/** 작은 기준: 딸기는 우유·빵하고만 같이 쓰였고, 콜라는 AI에 보내지 않는다. */
	private static MealPlanCombinationRules rules() {
		Map<String, IngredientCategoryCode> catalog = new LinkedHashMap<>();
		catalog.put("대파", IngredientCategoryCode.VEGETABLE);
		catalog.put("쪽파", IngredientCategoryCode.VEGETABLE);
		catalog.put("두부", IngredientCategoryCode.BEAN_TOFU);
		catalog.put("딸기", IngredientCategoryCode.FRUIT);
		catalog.put("우유", IngredientCategoryCode.DAIRY_EGG);
		catalog.put("식빵", IngredientCategoryCode.GRAIN);
		catalog.put("쌀", IngredientCategoryCode.GRAIN);
		catalog.put("간장", IngredientCategoryCode.SEASONING);
		catalog.put("계란", IngredientCategoryCode.DAIRY_EGG);
		catalog.put("달걀", IngredientCategoryCode.DAIRY_EGG);
		catalog.put("콜라", IngredientCategoryCode.BEVERAGE);
		catalog.put("생수", IngredientCategoryCode.BEVERAGE);
		catalog.put("바나나", IngredientCategoryCode.FRUIT);
		catalog.put("버터", IngredientCategoryCode.DAIRY_EGG);
		return new MealPlanCombinationRules(catalog, Map.of("계란", "달걀"),
				Map.of("콜라비", new MealPlanCombinationRules.Alias(null, IngredientCategoryCode.VEGETABLE),
						"간장소스", new MealPlanCombinationRules.Alias("간장", null)),
				Map.of("대파", "파", "쪽파", "파", "식빵", "빵"), Set.of("생수"),
				Set.of("딸기", "바나나", "우유", "빵", "버터"), Set.of("콜라"),
				Map.of("딸기", Set.of("우유", "빵"), "바나나", Set.of(), "콜라", Set.of("파")));
	}

	@Test
	void 식약처에서_같이_쓰인_짝은_통과하고_처음_보는_주의_재료_짝은_센다() {
		assertThat(rules().unseenCautionPairs(List.of("딸기", "우유"))).isZero();
		assertThat(rules().unseenCautionPairs(List.of("딸기", "두부"))).isEqualTo(1);
		// 딸기+두부, 딸기+쌀 두 짝. 두부+쌀은 주의 재료가 없어 보지 않는다
		assertThat(rules().unseenCautionPairs(List.of("딸기", "두부", "쌀"))).isEqualTo(2);
	}

	@Test
	void 주의_재료가_없으면_처음_보는_짝이어도_통과한다() {
		assertThat(rules().unseenCautionPairs(List.of("대파", "두부", "쌀", "달걀"))).isZero();
	}

	@Test
	void 아침_재료끼리는_처음_보는_짝이어도_통과한다() {
		// 바나나는 함께 쓰인 재료가 없지만 빵·버터는 아침 재료다
		assertThat(rules().unseenCautionPairs(List.of("바나나", "식빵", "버터"))).isZero();
		assertThat(rules().unseenCautionPairs(List.of("바나나", "달걀"))).isEqualTo(1);
	}

	@Test
	void 무리는_한_재료로_보고_같은_재료_다른_표기는_기준_이름으로_본다() {
		// 대파·쪽파는 "파" 하나. 콜라는 파와 같이 쓰였다
		assertThat(rules().unseenCautionPairs(List.of("콜라", "대파", "쪽파"))).isZero();
		// 계란 → 달걀, 바나나+달걀 한 짝
		assertThat(rules().unseenCautionPairs(List.of("바나나", "계란", "달걀"))).isEqualTo(1);
	}

	@Test
	void 이름_안에_사전_이름이_들어_있으면_가장_긴_것으로_맞춘다() {
		assertThat(rules().keyOf("다진 대파")).hasValue("파");
		assertThat(rules().keyOf("딸기(냉동)")).hasValue("딸기");
		assertThat(rules().keyOf("유기농 두부")).hasValue("두부");
		// 길이가 같으면 이름에서 더 뒤에 있는 것(한국어 합성어는 뒤가 중심)
		assertThat(rules().keyOf("두부딸기")).hasValue("딸기");
		assertThat(rules().keyOf("딸기두부")).hasValue("두부");
		// 한 글자 사전 이름(쌀)은 똑같을 때만 맞춘다
		assertThat(rules().keyOf("국산 쌀")).isEmpty();
	}

	@Test
	void 네_글자_이하_밥은_쌀로_보고_볶음밥은_그대로_둔다() {
		assertThat(rules().keyOf("밥")).hasValue("쌀");
		assertThat(rules().keyOf("현미밥")).hasValue("쌀");
		assertThat(rules().keyOf("볶음밥")).isEmpty();
	}

	@Test
	void 사전과_못_맞춘_이름_양념_물_고침표에서_뺀_이름은_검사하지_않는다() {
		assertThat(rules().keyOf("고수")).isEmpty();
		assertThat(rules().keyOf("간장")).isEmpty();
		assertThat(rules().keyOf("간장 소스")).isEmpty();
		assertThat(rules().keyOf("생수")).isEmpty();
		// 콜라비는 "콜라"가 들어 있지만 고침표에서 사전과 연결하지 않았다
		assertThat(rules().keyOf("콜라비")).isEmpty();
		assertThat(rules().unseenCautionPairs(List.of("딸기", "고수", "간장", "콜라비"))).isZero();
	}

	@Test
	void F1_보내지_않을_재료와_사전에_없는_음료는_빼고_나머지는_보낸다() {
		MealPlanCombinationRules rules = rules();
		assertThat(rules.excludedFromAi("콜라", IngredientCategoryCode.BEVERAGE)).isTrue();
		assertThat(rules.excludedFromAi("제로 콜라", IngredientCategoryCode.BEVERAGE)).isTrue();
		assertThat(rules.excludedFromAi("수제 레몬에이드", IngredientCategoryCode.BEVERAGE)).isTrue();
		assertThat(rules.excludedFromAi("생수", IngredientCategoryCode.BEVERAGE)).isFalse();
		assertThat(rules.excludedFromAi("대파", IngredientCategoryCode.VEGETABLE)).isFalse();
		// #145: 포함 규칙은 같은 카테고리만 본다. 채소로 등록한 콜라비는 콜라가 아니다
		assertThat(rules.excludedFromAi("콜라비", IngredientCategoryCode.VEGETABLE)).isFalse();
		// 카테고리를 모르면 사전 이름과 똑같을 때만 맞춘다
		assertThat(rules.excludedFromAi("콜라", null)).isTrue();
		assertThat(rules.excludedFromAi("제로 콜라", null)).isFalse();
	}

	@Test
	void 빈_기준은_아무것도_막지_않는다() {
		MealPlanCombinationRules empty = MealPlanCombinationRules.empty();
		assertThat(empty.unseenCautionPairs(List.of("딸기", "두부"))).isZero();
		assertThat(empty.excludedFromAi("콜라", null)).isFalse();
	}
}
