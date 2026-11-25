package com.remake.recipe.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.recipe.application.port.out.GeneratedRecipe;
import com.remake.recipe.application.port.out.RecipeAiRequest;
import com.remake.recipe.domain.CustomOptions;
import com.remake.recipe.domain.Recipe;
import com.remake.recipe.domain.RecipeIngredient;
import com.remake.recipe.domain.RecipeSource;
import com.remake.recipe.domain.RecipeUnit;

class GeneratedRecipeValidatorTest {

	private static final Long USER_ID = 1L;
	private static final Long EGG_ID = 10L;
	private static final Long ONION_ID = 11L;
	private static final List<String> STEPS = List.of("재료를 손질한다.", "볶는다.");

	private static RecipeAiRequest quickRequest(boolean supplementEnabled) {
		return new RecipeAiRequest(RecipeSource.QUICK, 2, supplementEnabled, List.of(
				new RecipeAiRequest.FridgeItem(EGG_ID, "계란", new BigDecimal("6"), "EA", LocalDate.of(2026, 3, 6), true),
				new RecipeAiRequest.FridgeItem(ONION_ID, "양파", new BigDecimal("2"), "EA", LocalDate.of(2026, 3, 20),
						false)),
				null, List.of());
	}

	private static GeneratedRecipe generated(GeneratedRecipe.Ingredient... ingredients) {
		return new GeneratedRecipe("계란 양파볶음", 15, "약불로 볶아요.", List.of(ingredients), STEPS);
	}

	private static GeneratedRecipe.Ingredient item(Long fridgeId, String name, String amount, String unit) {
		return new GeneratedRecipe.Ingredient(fridgeId, name, amount == null ? null : new BigDecimal(amount), unit);
	}

	@Test
	void 냉장고_재료로만_만든_응답은_요청한_인분으로_레시피가_된다() {
		Recipe recipe = GeneratedRecipeValidator.toRecipe(USER_ID, quickRequest(false),
				generated(item(EGG_ID, "계란", "3", "EA"), item(ONION_ID, "양파", "1", "ea")));

		assertThat(recipe.userId()).isEqualTo(USER_ID);
		assertThat(recipe.source()).isEqualTo(RecipeSource.QUICK);
		assertThat(recipe.servings()).isEqualTo(2);
		assertThat(recipe.cookMinutes()).isEqualTo(15);
		assertThat(recipe.ingredients()).extracting(RecipeIngredient::ingredientId).containsExactly(EGG_ID, ONION_ID);
		assertThat(recipe.ingredients().get(1).unit()).isEqualTo(RecipeUnit.EA);
		assertThat(recipe.steps()).isEqualTo(STEPS);
	}

	@Test
	void 목록에_없는_단위의_재료는_그_줄만_뺀다() {
		Recipe recipe = GeneratedRecipeValidator.toRecipe(USER_ID, quickRequest(false),
				generated(item(EGG_ID, "계란", "3", "EA"), item(ONION_ID, "양파", "1", "BOWL"),
						item(ONION_ID, "양파", "1", null)));

		assertThat(recipe.ingredients()).extracting(RecipeIngredient::name).containsExactly("계란");
	}

	@Test
	void 쓸_수_있는_재료가_하나도_남지_않으면_형식_오류다() {
		assertInvalid(quickRequest(false), generated(item(EGG_ID, "계란", "3", "BOWL")));
	}

	@Test
	void 요청에_없는_냉장고_재료_id는_냉장고에_없는_재료로_본다() {
		Recipe recipe = GeneratedRecipeValidator.toRecipe(USER_ID, quickRequest(true),
				generated(item(EGG_ID, "계란", "3", "EA"), item(999L, "베이컨", "100", "G")));

		assertThat(recipe.ingredients().get(1).ingredientId()).isNull();
	}

	@Test
	void 재료_보충이_꺼져_있으면_냉장고에_없는_재료는_형식_오류다() {
		assertInvalid(quickRequest(false), generated(item(EGG_ID, "계란", "3", "EA"), item(null, "베이컨", "100", "G")));
	}

	@Test
	void 재료_보충이_꺼져_있으면_냉장고에_없는_기본_양념과_꼬집_약간도_형식_오류다() {
		assertInvalid(quickRequest(false), generated(item(EGG_ID, "계란", "3", "EA"), item(null, "식용유", "1", "TBSP")));
		assertInvalid(quickRequest(false), generated(item(EGG_ID, "계란", "3", "EA"), item(null, "후춧가루", "1", "PINCH")));
		assertInvalid(quickRequest(false), generated(item(EGG_ID, "계란", "3", "EA"), item(null, "소금", null, "TO_TASTE")));
	}

	@Test
	void 재료_보충이_꺼져_있어도_물은_냉장고에_없이_쓸_수_있다() {
		Recipe recipe = GeneratedRecipeValidator.toRecipe(USER_ID, quickRequest(false),
				generated(item(EGG_ID, "계란", "3", "EA"), item(null, " 물 ", "500", "ML")));

		assertThat(recipe.ingredients().get(1).ingredientId()).isNull();
		assertInvalid(quickRequest(false), generated(item(EGG_ID, "계란", "3", "EA"), item(null, "생수", "500", "ML")));
	}

	@Test
	void 냉장고에_있는_재료는_꼬집_약간으로도_쓸_수_있고_약간은_양을_비운다() {
		Recipe recipe = GeneratedRecipeValidator.toRecipe(USER_ID, quickRequest(false),
				generated(item(EGG_ID, "계란", "2", "TO_TASTE")));

		assertThat(recipe.ingredients().get(0).amount()).isNull();
	}

	@Test
	void 재료_보충이_켜져_있으면_냉장고에_없는_재료도_쓸_수_있다() {
		Recipe recipe = GeneratedRecipeValidator.toRecipe(USER_ID, quickRequest(true),
				generated(item(EGG_ID, "계란", "3", "EA"), item(null, "베이컨", "100", "G")));

		assertThat(recipe.ingredients().get(1).ingredientId()).isNull();
	}

	@Test
	void 양은_소수_셋째_자리로_반올림하고_0_이하가_되면_형식_오류다() {
		Recipe recipe = GeneratedRecipeValidator.toRecipe(USER_ID, quickRequest(false),
				generated(item(EGG_ID, "계란", "1.23456", "EA")));

		assertThat(recipe.ingredients().get(0).amount()).isEqualByComparingTo("1.235");
		BigDecimal whole = GeneratedRecipeValidator.toRecipe(USER_ID, quickRequest(false),
				generated(item(EGG_ID, "계란", "300.000", "EA"))).ingredients().get(0).amount();
		assertThat(whole.toString()).as("지수 표기(3E+2)가 아니라 300").isEqualTo("300");
		assertInvalid(quickRequest(false), generated(item(EGG_ID, "계란", "0.0001", "EA")));
		assertInvalid(quickRequest(false), generated(item(EGG_ID, "계란", "-1", "EA")));
	}

	@Test
	void 필수_값이_없으면_형식_오류다() {
		RecipeAiRequest request = quickRequest(false);
		List<GeneratedRecipe.Ingredient> ingredients = List.of(item(EGG_ID, "계란", "3", "EA"));

		assertInvalid(request, null);
		assertInvalid(request, new GeneratedRecipe(null, 15, null, ingredients, STEPS));
		assertInvalid(request, new GeneratedRecipe("계란찜", null, null, ingredients, STEPS));
		assertInvalid(request, new GeneratedRecipe("계란찜", 0, null, ingredients, STEPS));
		assertInvalid(request, new GeneratedRecipe("계란찜", 15, null, null, STEPS));
		assertInvalid(request, new GeneratedRecipe("계란찜", 15, null, ingredients, null));
		assertInvalid(request, new GeneratedRecipe("계란찜", 15, null, ingredients, List.of()));
		List<String> blankStep = new ArrayList<>(STEPS);
		blankStep.add(" ");
		assertInvalid(request, new GeneratedRecipe("계란찜", 15, null, ingredients, blankStep));
		assertInvalid(request, generated(item(EGG_ID, " ", "3", "EA")));
	}

	@Test
	void 커스텀_생성은_선택값을_그대로_레시피에_남긴다() {
		CustomOptions options = new CustomOptions(3, List.of(EGG_ID), null, null, null, null, null, null);
		RecipeAiRequest request = new RecipeAiRequest(RecipeSource.CUSTOM, 3, false,
				quickRequest(false).fridgeIngredients(), options, List.of());

		Recipe recipe = GeneratedRecipeValidator.toRecipe(USER_ID, request, generated(item(EGG_ID, "계란", "3", "EA")));

		assertThat(recipe.source()).isEqualTo(RecipeSource.CUSTOM);
		assertThat(recipe.servings()).isEqualTo(3);
		assertThat(recipe.customOptions()).isEqualTo(options);
	}

	private static void assertInvalid(RecipeAiRequest request, GeneratedRecipe generated) {
		assertThatThrownBy(() -> GeneratedRecipeValidator.toRecipe(USER_ID, request, generated))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_INVALID_RESPONSE));
	}

	@Test
	void 재료_카테고리는_사전_냉장고_재료_AI_순서로_정한다() {
		RecipeAiRequest request = new RecipeAiRequest(RecipeSource.QUICK, 2, true, List.of(
				new RecipeAiRequest.FridgeItem(EGG_ID, "계란", new BigDecimal("6"), "EA", LocalDate.of(2026, 3, 6), true,
						IngredientCategoryCode.DAIRY_EGG),
				new RecipeAiRequest.FridgeItem(ONION_ID, "양파", new BigDecimal("2"), "EA", LocalDate.of(2026, 3, 20),
						false, IngredientCategoryCode.ETC)),
				null, List.of());
		// 사전에는 "양파"만 VEGETABLE로 있다고 본다
		Function<String, Optional<IngredientCategoryCode>> catalog = name -> "양파".equals(name)
				? Optional.of(IngredientCategoryCode.VEGETABLE) : Optional.empty();

		Recipe recipe = GeneratedRecipeValidator.toRecipe(USER_ID, request, generated(
				new GeneratedRecipe.Ingredient(EGG_ID, "계란", BigDecimal.ONE, "EA", "MEAT"),
				new GeneratedRecipe.Ingredient(ONION_ID, "양파", BigDecimal.ONE, "EA", null),
				new GeneratedRecipe.Ingredient(null, "청상추", BigDecimal.ONE, "EA", " vegetable "),
				new GeneratedRecipe.Ingredient(null, "알수없는재료", BigDecimal.ONE, "EA", "SNACK")), catalog);

		assertThat(recipe.ingredients()).extracting(RecipeIngredient::categoryCode).containsExactly(
				IngredientCategoryCode.DAIRY_EGG, // 냉장고 재료의 카테고리(AI 값보다 먼저)
				IngredientCategoryCode.VEGETABLE, // 사전(냉장고 재료의 ETC보다 먼저)
				IngredientCategoryCode.VEGETABLE, // AI 값(공백·대소문자 맞춤)
				null); // 12개 코드에 없는 AI 값은 비움(실패 아님)
	}
}
