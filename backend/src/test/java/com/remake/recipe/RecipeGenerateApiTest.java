package com.remake.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.ingredient.domain.IngredientStatus;
import com.remake.recipe.application.port.out.GeneratedRecipe;
import com.remake.recipe.application.port.out.RecipeAiRequest;
import com.remake.recipe.domain.RecipeSource;
import com.remake.support.IntegrationTestSupport;

// API 28 레시피 생성. Gemini 어댑터는 가짜(IntegrationTestSupport). 기본 시각 2026-03-04 10:00(서울), 일반 회원 하루 2회
class RecipeGenerateApiTest extends IntegrationTestSupport {

	private static final String URL = "/api/v1/user/recipes/generate";
	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	private Long userId;

	@BeforeEach
	void setUp() {
		userId = createUser();
	}

	private ResultActions generate(String body) throws Exception {
		return mockMvc.perform(as(userId, post(URL)).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private MockHttpServletRequestBuilder as(Long id, MockHttpServletRequestBuilder builder) {
		return builder.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, id);
	}

	private Long fridge(String category, String name, int daysLeft) {
		return createIngredient(userId, category, name, TODAY.plusDays(daysLeft), IngredientStatus.ACTIVE);
	}

	private static GeneratedRecipe aiRecipe(Long... fridgeIds) {
		List<GeneratedRecipe.Ingredient> ingredients = new java.util.ArrayList<>();
		for (Long id : fridgeIds) {
			ingredients.add(new GeneratedRecipe.Ingredient(id, "재료" + id, BigDecimal.ONE, "EA"));
		}
		ingredients.add(new GeneratedRecipe.Ingredient(null, "소금", null, "TO_TASTE"));
		return new GeneratedRecipe("계란 양파볶음", 15, "약불로 볶아요.", ingredients, List.of("썬다.", "볶는다."));
	}

	private int recipeRows() {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM recipes WHERE user_id = ?", Integer.class, userId);
	}

	private int usageRows() {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usage_logs WHERE user_id = ?", Integer.class, userId);
	}

	private String eventProperties(String name) {
		awaitEventWrites();
		return jdbcTemplate.queryForObject("SELECT properties::text FROM events WHERE event_name = ? AND user_id = ?",
				String.class, name, userId);
	}

	private RecipeAiRequest sentRequest(int times) {
		ArgumentCaptor<RecipeAiRequest> captor = ArgumentCaptor.forClass(RecipeAiRequest.class);
		verify(geminiRecipeAdapter, times(times)).generate(captor.capture());
		return captor.getValue();
	}

	@Test
	void 로그인하지_않으면_401이다() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"QUICK\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 빠른_생성은_가구원_수와_소비기한_지나지_않은_냉장고_재료로_만들고_저장과_차감을_한다() throws Exception {
		jdbcTemplate.update("UPDATE users SET household_size = 3 WHERE id = ?", userId);
		Long egg = fridge("DAIRY_EGG", "계란", 1);
		Long onion = fridge("VEGETABLE", "양파", 10);
		fridge("MEAT", "상한 고기", -1);
		when(geminiRecipeAdapter.generate(any())).thenReturn(aiRecipe(egg, onion));

		generate("{\"mode\":\"QUICK\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.title").value("계란 양파볶음"))
				.andExpect(jsonPath("$.servings").value(3))
				.andExpect(jsonPath("$.cookMinutes").value(15))
				.andExpect(jsonPath("$.ingredients.length()").value(3))
				.andExpect(jsonPath("$.ingredients[2].amount").doesNotExist())
				.andExpect(jsonPath("$.ingredients[2].unit").value("TO_TASTE"))
				.andExpect(jsonPath("$.steps[1].stepNo").value(2))
				.andExpect(jsonPath("$.saved").value(false))
				.andExpect(jsonPath("$.favorite").value(false));

		RecipeAiRequest sent = sentRequest(1);
		assertThat(sent.source()).isEqualTo(RecipeSource.QUICK);
		assertThat(sent.servings()).isEqualTo(3);
		assertThat(sent.supplementEnabled()).isFalse();
		assertThat(sent.fridgeIngredients()).extracting(RecipeAiRequest.FridgeItem::ingredientId)
				.as("소비기한이 지난 재료는 보내지 않는다").containsExactly(egg, onion);
		assertThat(sent.fridgeIngredients().get(0).expiringSoon()).isTrue();
		assertThat(sent.fridgeIngredients().get(1).expiringSoon()).isFalse();
		assertThat(recipeRows()).isEqualTo(1);
		assertThat(usageRows()).isEqualTo(1);
		assertThat(eventProperties("recipe_generated"))
				.contains("\"recipe_mode\": \"QUICK\"", "\"is_regenerated\": false", "\"expiring_used_count\": 1",
						"\"shortage_count\": 1", "\"remaining_quota\": 1");
	}

	@Test
	void 커스텀은_고른_재료와_인원과_옵션으로_만들고_선택값을_저장한다() throws Exception {
		Long egg = fridge("DAIRY_EGG", "계란", 5);
		Long onion = fridge("VEGETABLE", "양파", 5);
		fridge("MEAT", "돼지고기", 5);
		when(geminiRecipeAdapter.generate(any())).thenReturn(aiRecipe(egg, onion));

		generate("""
				{"mode":"CUSTOM","servings":2,"ingredientIds":[%d,%d],"taste":"SPICY","cuisine":"KOREAN","allergenCodes":["SHRIMP"]}
				""".formatted(egg, onion))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.servings").value(2));

		RecipeAiRequest sent = sentRequest(1);
		assertThat(sent.fridgeIngredients()).extracting(RecipeAiRequest.FridgeItem::ingredientId)
				.containsExactly(egg, onion);
		assertThat(sent.customOptions().taste().name()).isEqualTo("SPICY");
		assertThat(sent.excludedAllergenCodes()).containsExactly("SHRIMP");
		assertThat(jdbcTemplate.queryForObject("SELECT custom_options->>'taste' FROM recipes WHERE user_id = ?",
				String.class, userId)).isEqualTo("SPICY");
	}

	@Test
	void 커스텀은_인원이_필요하고_내_재료가_아니면_404다() throws Exception {
		Long othersEgg = createIngredient(createUser(), "DAIRY_EGG", "남의 계란", TODAY.plusDays(5),
				IngredientStatus.ACTIVE);

		generate("{\"mode\":\"CUSTOM\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		generate("{\"mode\":\"CUSTOM\",\"servings\":2,\"ingredientIds\":[%d]}".formatted(othersEgg))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("INGREDIENT_NOT_FOUND"));
		generate("{\"mode\":\"MEAL_PLAN\"}")
				.andExpect(status().isBadRequest());
		verify(geminiRecipeAdapter, never()).generate(any());
	}

	@Test
	void 재료_보충이_꺼져_있고_양념을_뺀_재료가_2개_미만이면_AI를_부르지_않고_NO_INGREDIENTS다() throws Exception {
		fridge("VEGETABLE", "양파", 5);
		fridge("SEASONING", "간장", 100);
		fridge("SEASONING", "고추장", 100);
		fridge("MEAT", "상한 고기", -1);

		generate("{\"mode\":\"QUICK\"}")
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("NO_INGREDIENTS"));

		verify(geminiRecipeAdapter, never()).generate(any());
		assertThat(usageRows()).isZero();
		assertThat(eventProperties("recipe_generate_failed")).contains("\"error_code\": \"NO_INGREDIENTS\"");
	}

	@Test
	void 재료_보충이_켜져_있으면_냉장고가_비어도_만든다() throws Exception {
		jdbcTemplate.update("UPDATE users SET recipe_supplement_enabled = true WHERE id = ?", userId);
		when(geminiRecipeAdapter.generate(any())).thenReturn(aiRecipe());

		generate("{\"mode\":\"QUICK\"}").andExpect(status().isCreated());

		assertThat(sentRequest(1).supplementEnabled()).isTrue();
	}

	@Test
	void 횟수를_다_쓰면_AI를_부르지_않고_429와_details를_준다() throws Exception {
		fridge("DAIRY_EGG", "계란", 5);
		fridge("VEGETABLE", "양파", 5);
		for (int i = 0; i < 2; i++) {
			jdbcTemplate.update("INSERT INTO usage_logs (user_id, type, created_at) VALUES (?, 'RECIPE', ?)", userId,
					LocalDateTime.of(2026, 3, 4, 9, i));
		}

		generate("{\"mode\":\"QUICK\"}")
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("GENERATION_LIMIT_EXCEEDED"))
				.andExpect(jsonPath("$.details.remaining").value(0))
				.andExpect(jsonPath("$.details.resetsAt").value("2026-03-05T00:00:00+09:00"));

		verify(geminiRecipeAdapter, never()).generate(any());
		assertThat(eventProperties("quota_exceeded"))
				.contains("\"limit_feature\": \"RECIPE\"", "\"limit_total\": 2", "\"used_count\": 2");
	}

	@Test
	void AI가_한_번_실패하면_다시_시도하고_성공하면_한_번만_차감한다() throws Exception {
		Long egg = fridge("DAIRY_EGG", "계란", 5);
		Long onion = fridge("VEGETABLE", "양파", 5);
		when(geminiRecipeAdapter.generate(any()))
				.thenThrow(new BusinessException(ErrorCode.AI_UPSTREAM_ERROR))
				.thenReturn(aiRecipe(egg, onion));

		generate("{\"mode\":\"QUICK\"}").andExpect(status().isCreated());

		sentRequest(2);
		assertThat(usageRows()).isEqualTo(1);
	}

	@Test
	void 형식이_틀린_응답도_다시_시도하고_두_번_다_실패하면_저장도_차감도_하지_않는다() throws Exception {
		fridge("DAIRY_EGG", "계란", 5);
		fridge("VEGETABLE", "양파", 5);
		GeneratedRecipe noSteps = new GeneratedRecipe("계란찜", 10, null,
				List.of(new GeneratedRecipe.Ingredient(null, "계란", BigDecimal.ONE, "EA")), List.of());
		when(geminiRecipeAdapter.generate(any())).thenReturn(noSteps);

		generate("{\"mode\":\"QUICK\"}")
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("AI_INVALID_RESPONSE"));

		sentRequest(2);
		assertThat(recipeRows()).isZero();
		assertThat(usageRows()).isZero();
		assertThat(eventProperties("recipe_generate_failed"))
				.contains("\"error_code\": \"AI_INVALID_RESPONSE\"", "\"is_regenerated\": false");
	}

	@Test
	void 시간_초과가_두_번이면_504이고_차감하지_않는다() throws Exception {
		fridge("DAIRY_EGG", "계란", 5);
		fridge("VEGETABLE", "양파", 5);
		when(geminiRecipeAdapter.generate(any())).thenThrow(new BusinessException(ErrorCode.AI_TIMEOUT));

		generate("{\"mode\":\"QUICK\"}")
				.andExpect(status().isGatewayTimeout())
				.andExpect(jsonPath("$.code").value("AI_TIMEOUT"));

		assertThat(usageRows()).isZero();
	}
}
