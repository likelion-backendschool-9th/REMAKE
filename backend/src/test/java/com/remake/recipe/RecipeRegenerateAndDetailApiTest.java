package com.remake.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.recipe.application.port.out.GeneratedRecipe;
import com.remake.recipe.application.port.out.RecipeAiRequest;
import com.remake.recipe.application.port.out.SaveRecipePort;
import com.remake.recipe.domain.CustomOptions;
import com.remake.recipe.domain.Recipe;
import com.remake.recipe.domain.RecipeIngredient;
import com.remake.recipe.domain.RecipeSource;
import com.remake.recipe.domain.RecipeUnit;
import com.remake.recipe.domain.Taste;
import com.remake.support.IntegrationTestSupport;
import com.remake.user.domain.AllergenCode;

// API 29 다시 받기, API 30 상세. Gemini 어댑터는 가짜. 기본 시각 2026-03-04 10:00(서울)
class RecipeRegenerateAndDetailApiTest extends IntegrationTestSupport {

	private static final String URL = "/api/v1/user/recipes/";
	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	@Autowired
	private SaveRecipePort saveRecipePort;

	private Long userId;

	@BeforeEach
	void setUp() {
		userId = createUser();
	}

	private MockHttpServletRequestBuilder as(Long id, MockHttpServletRequestBuilder builder) {
		return builder.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, id);
	}

	private Long fridge(String category, String name) {
		return createIngredient(userId, category, name, TODAY.plusDays(5));
	}

	private Recipe saveRecipe(Long owner, RecipeSource source, CustomOptions options) {
		return transactionTemplate.execute(status -> saveRecipePort.save(Recipe.create(owner, source, "계란말이", 2, 10,
				"약불", options, List.of(new RecipeIngredient(null, "계란", new BigDecimal("3"), RecipeUnit.EA),
						new RecipeIngredient(null, "소금", null, RecipeUnit.TO_TASTE)),
				List.of("푼다.", "만다."))));
	}

	private static GeneratedRecipe aiRecipe(Long... fridgeIds) {
		List<GeneratedRecipe.Ingredient> ingredients = new java.util.ArrayList<>();
		for (Long id : fridgeIds) {
			ingredients.add(new GeneratedRecipe.Ingredient(id, "재료" + id, BigDecimal.ONE, "EA"));
		}
		return new GeneratedRecipe("새 레시피", 20, null, ingredients, List.of("만든다."));
	}

	private RecipeAiRequest sentRequest() {
		ArgumentCaptor<RecipeAiRequest> captor = ArgumentCaptor.forClass(RecipeAiRequest.class);
		verify(geminiRecipeAdapter).generate(captor.capture());
		return captor.getValue();
	}

	@Test
	void 상세는_내_레시피를_재료_순서와_단계_번호로_돌려준다() throws Exception {
		Recipe recipe = saveRecipe(userId, RecipeSource.QUICK, null);

		mockMvc.perform(as(userId, get(URL + recipe.id())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(recipe.id()))
				.andExpect(jsonPath("$.title").value("계란말이"))
				.andExpect(jsonPath("$.servings").value(2))
				.andExpect(jsonPath("$.cookMinutes").value(10))
				.andExpect(jsonPath("$.tips").value("약불"))
				.andExpect(jsonPath("$.ingredients[0].name").value("계란"))
				.andExpect(jsonPath("$.ingredients[0].amount").value(3))
				.andExpect(jsonPath("$.ingredients[0].unit").value("EA"))
				.andExpect(jsonPath("$.ingredients[1].unit").value("TO_TASTE"))
				.andExpect(jsonPath("$.steps[0].stepNo").value(1))
				.andExpect(jsonPath("$.steps[1].content").value("만다."))
				.andExpect(jsonPath("$.saved").value(false))
				.andExpect(jsonPath("$.favorite").value(false))
				.andExpect(jsonPath("$.shareable").value(true));
	}

	@Test
	void 상세는_없거나_남의_레시피면_404다() throws Exception {
		Recipe others = saveRecipe(createUser(), RecipeSource.QUICK, null);

		mockMvc.perform(as(userId, get(URL + others.id())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));
		mockMvc.perform(as(userId, get(URL + 999999)))
				.andExpect(status().isNotFound());
	}

	@Test
	void 커스텀_다시_받기는_저장해_둔_선택값으로_만들고_없어진_재료는_뺀다() throws Exception {
		Long egg = fridge("DAIRY_EGG", "계란");
		Long onion = fridge("VEGETABLE", "양파");
		Long gone = fridge("MEAT", "다 먹은 고기");
		CustomOptions options = new CustomOptions(3, List.of(egg, onion, gone), Taste.SPICY, null, null, null, null,
				List.of(AllergenCode.SHRIMP));
		Recipe original = saveRecipe(userId, RecipeSource.CUSTOM, options);
		jdbcTemplate.update("DELETE FROM ingredients WHERE id = ?", gone);
		when(geminiRecipeAdapter.generate(any())).thenReturn(aiRecipe(egg, onion));

		mockMvc.perform(as(userId, post(URL + original.id() + "/regenerate")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("새 레시피"))
				.andExpect(jsonPath("$.servings").value(3));

		RecipeAiRequest sent = sentRequest();
		assertThat(sent.source()).isEqualTo(RecipeSource.CUSTOM);
		assertThat(sent.servings()).isEqualTo(3);
		assertThat(sent.customOptions().taste()).isEqualTo(Taste.SPICY);
		assertThat(sent.excludedAllergenCodes()).containsExactly(AllergenCode.SHRIMP);
		assertThat(sent.fridgeIngredients()).extracting(RecipeAiRequest.FridgeItem::ingredientId)
				.containsExactly(egg, onion);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM recipes WHERE user_id = ?", Integer.class,
				userId)).as("원래 레시피는 남고 새 레시피가 생긴다").isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usage_logs WHERE user_id = ?", Integer.class,
				userId)).isEqualTo(1);
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT properties->>'is_regenerated' FROM events WHERE event_name = 'recipe_generated' AND user_id = ?",
				String.class, userId)).isEqualTo("true");
	}

	@Test
	void 빠른_다시_받기는_지금의_냉장고로_만든다() throws Exception {
		Recipe original = saveRecipe(userId, RecipeSource.QUICK, null);
		Long egg = fridge("DAIRY_EGG", "계란");
		Long onion = fridge("VEGETABLE", "양파");
		when(geminiRecipeAdapter.generate(any())).thenReturn(aiRecipe(egg, onion));

		mockMvc.perform(as(userId, post(URL + original.id() + "/regenerate")))
				.andExpect(status().isCreated());

		RecipeAiRequest sent = sentRequest();
		assertThat(sent.source()).isEqualTo(RecipeSource.QUICK);
		assertThat(sent.customOptions()).isNull();
		assertThat(sent.fridgeIngredients()).extracting(RecipeAiRequest.FridgeItem::ingredientId)
				.containsExactly(egg, onion);
	}

	@Test
	void 다시_받기는_남의_레시피면_404이고_식단표_레시피는_400이다() throws Exception {
		Recipe others = saveRecipe(createUser(), RecipeSource.QUICK, null);
		Recipe mealPlan = saveRecipe(userId, RecipeSource.MEAL_PLAN, null);

		mockMvc.perform(as(userId, post(URL + others.id() + "/regenerate")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));
		mockMvc.perform(as(userId, post(URL + mealPlan.id() + "/regenerate")))
				.andExpect(status().isBadRequest());
		verify(geminiRecipeAdapter, never()).generate(any());
	}
}
