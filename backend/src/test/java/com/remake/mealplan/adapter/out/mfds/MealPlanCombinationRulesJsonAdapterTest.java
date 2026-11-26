package com.remake.mealplan.adapter.out.mfds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.mealplan.domain.MealPlanCombinationRules;

import tools.jackson.databind.json.JsonMapper;

/**
 * 실제 combination-check.json으로 판정한다. 기대값은 정리 스크립트를 만들 때 파이썬으로 한 시뮬레이션과 같다
 * (11-24~26 실측 AI 레시피 991개 중 걸림 0개, 엉터리 예시 16개 중 14개 걸림).
 */
class MealPlanCombinationRulesJsonAdapterTest {

	private static MealPlanCombinationRules rules;

	@BeforeAll
	static void load() {
		rules = MealPlanCombinationRulesJsonAdapter.read(JsonMapper.builder().build(),
				MealPlanCombinationRulesJsonAdapter.RESOURCE);
	}

	@Test
	void 실제_파일을_읽는다() {
		assertThat(rules.partners()).hasSize(66).containsKeys("딸기", "콜라", "초콜릿", "요거트", "견과류");
		assertThat(rules.excludedFromAi()).hasSize(18);
		assertThat(rules.catalog()).containsEntry("대파", IngredientCategoryCode.VEGETABLE);
	}

	@Test
	void 엉터리_조합_16개_중_14개를_잡는다() {
		Map<String, List<String>> caught = Map.ofEntries(
				Map.entry("토마토초코밥", List.of("토마토", "초콜릿", "쌀")),
				Map.entry("콜라 김치찌개", List.of("콜라", "배추김치", "돼지고기", "두부")),
				Map.entry("딸기 된장국", List.of("딸기", "두부", "된장")),
				Map.entry("아이스크림 라면", List.of("아이스크림", "라면", "대파")),
				Map.entry("잼 두부조림", List.of("잼", "두부", "간장")),
				Map.entry("초콜릿 닭가슴살 볶음", List.of("초콜릿", "닭가슴살", "양파")),
				Map.entry("요거트 김치볶음밥", List.of("요거트", "배추김치", "쌀")),
				Map.entry("사이다 불고기", List.of("사이다", "쇠고기", "양파")),
				Map.entry("과자 계란찜", List.of("과자", "계란")),
				Map.entry("귤 돼지고기 찌개", List.of("귤", "돼지고기", "두부")),
				Map.entry("초코 시리얼 김밥", List.of("초콜릿", "시리얼", "김", "쌀")),
				Map.entry("딸기잼 라면", List.of("잼", "라면")),
				// 초콜릿 다른 표기는 고침표로 초콜릿에 맞춘다
				Map.entry("초코칩 김치볶음밥", List.of("초코칩", "배추김치", "밥")),
				Map.entry("누텔라 닭볶음", List.of("누텔라", "닭다리", "양파")));
		caught.forEach((title, names) -> assertThat(rules.unseenCautionPairs(names)).as(title).isPositive());

		// 놓치는 2개: 식약처에 바나나+달걀·쌀 짝이 있고, 우유는 주의 재료가 아니다
		assertThat(rules.unseenCautionPairs(List.of("바나나", "계란", "쌀", "대파"))).isZero();
		assertThat(rules.unseenCautionPairs(List.of("우유", "배추김치", "돼지고기"))).isZero();
	}

	@Test
	void 멀쩡한_레시피는_통과한다() {
		Map<String, List<String>> normal = Map.ofEntries(
				Map.entry("바나나토스트", List.of("바나나", "식빵", "버터")),
				Map.entry("우유 시리얼", List.of("시리얼", "우유", "바나나")),
				Map.entry("딸기 요거트", List.of("딸기", "그릭요거트", "꿀")),
				Map.entry("김치찌개", List.of("배추김치", "돼지고기 앞다리살", "두부", "대파", "고춧가루")),
				Map.entry("사과 돼지고기 볶음", List.of("사과", "돼지고기", "양파")),
				Map.entry("파인애플 볶음밥", List.of("파인애플", "밥", "달걀", "대파")),
				Map.entry("배 소불고기", List.of("배", "쇠고기", "양파", "간장")),
				Map.entry("레몬 연어구이", List.of("레몬", "연어", "버터")),
				Map.entry("두유 콩국수", List.of("두유", "소면", "오이")),
				// 견과류는 한 무리: 식약처의 멸치+땅콩·호두 짝을 아몬드도 쓴다(11-26 결정)
				Map.entry("아몬드 멸치볶음", List.of("아몬드", "멸치", "간장", "물엿")),
				Map.entry("캐슈넛 닭볶음", List.of("캐슈넛", "닭가슴살", "양파")),
				// 콜라비는 사전 "콜라"로 맞추지 않는다(고침표)
				Map.entry("콜라비 무침", List.of("콜라비", "오이", "양파")),
				// 초콜릿은 아침 재료(잼·땅콩버터와 같은 쪽): 빵·바나나와 짝이 처음이어도 허용(11-26 결정)
				Map.entry("초코 바나나 토스트", List.of("초코시럽", "바나나", "식빵")));
		normal.forEach((title, names) -> assertThat(rules.unseenCautionPairs(names)).as(title).isZero());
	}

	@Test
	void F1_식약처_요리에_안_나오는_냉장고_재료와_사전에_없는_음료는_AI에_보내지_않는다() {
		for (String name : List.of("콜라", "캔커피", "아메리카노", "홍차", "보리차", "옥수수차", "이온음료", "에너지드링크",
				"요구르트음료", "두유음료", "식혜", "초콜릿", "아이스크림", "젤리", "과자", "케이크", "쿠키", "잼")) {
			assertThat(rules.excludedFromAi(name, null)).as(name).isTrue();
		}
		assertThat(rules.excludedFromAi("수제 레몬에이드", IngredientCategoryCode.BEVERAGE)).isTrue();
		for (String name : List.of("생수", "우유", "딸기", "두유", "요거트", "대파")) {
			assertThat(rules.excludedFromAi(name, null)).as(name).isFalse();
		}
		assertThat(rules.excludedFromAi("콜라비", IngredientCategoryCode.VEGETABLE)).isFalse();
	}

	/**
	 * 정리 스크립트(파이썬)와 같은 판정인지: 스크립트가 식약처 재료 이름 전부·예시 이름의 검사 키와
	 * 예시·무작위 레시피의 처음 보는 주의 재료 짝 수를 test resources에 써 둔다. 한쪽 규칙만 고치면 여기서 깨진다.
	 */
	@Test
	void 정리_스크립트와_판정이_같다() throws Exception {
		Expected expected;
		try (InputStream in = new ClassPathResource(EXPECTED).getInputStream()) {
			expected = JsonMapper.builder().build().readValue(in, Expected.class);
		}
		assertThat(expected.keys()).hasSizeGreaterThan(1000);
		SoftAssertions soft = new SoftAssertions();
		expected.keys().forEach((name, key) -> soft.assertThat(rules.keyOf(name)).as(name)
				.isEqualTo(Optional.ofNullable(key)));
		for (ExpectedRecipe recipe : expected.recipes()) {
			soft.assertThat(rules.unseenCautionPairs(recipe.ingredients())).as(recipe.name())
					.isEqualTo(recipe.unseenCautionPairs());
		}
		soft.assertAll();
	}

	/** 스크립트가 만든 판정 비교용 기대값. 고치지 말고 스크립트를 다시 돌린다. */
	private static final String EXPECTED = "mfds/combination-check-expected.json";

	record Expected(Map<String, String> keys, List<ExpectedRecipe> recipes) {
	}

	record ExpectedRecipe(String name, List<String> ingredients, int unseenCautionPairs) {
	}

	@Test
	void 파일이_없으면_앱이_뜨지_않게_예외를_던진다() {
		assertThatThrownBy(() -> MealPlanCombinationRulesJsonAdapter.read(JsonMapper.builder().build(),
				"mfds/none.json")).isInstanceOf(IllegalStateException.class);
	}
}
