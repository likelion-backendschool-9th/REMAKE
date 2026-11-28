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
import com.remake.recipe.application.port.out.GeneratedRecipe;
import com.remake.recipe.application.port.out.RecipeAiRequest;
import com.remake.recipe.domain.RecipeSource;
import com.remake.support.IntegrationTestSupport;
import com.remake.user.domain.AllergenCode;

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
		return createIngredient(userId, category, name, TODAY.plusDays(daysLeft));
	}

	private void excludeInSettings(String... codes) {
		for (String code : codes) {
			jdbcTemplate.update("INSERT INTO user_excluded_ingredients (user_id, allergen_code) VALUES (?, ?)", userId, code);
		}
	}

	private static GeneratedRecipe aiRecipe(Long... fridgeIds) {
		List<GeneratedRecipe.Ingredient> ingredients = new java.util.ArrayList<>();
		for (Long id : fridgeIds) {
			ingredients.add(new GeneratedRecipe.Ingredient(id, "재료" + id, BigDecimal.ONE, "EA"));
		}
		return new GeneratedRecipe("계란 양파볶음", 15, "약불로 볶아요.", ingredients, List.of("썬다.", "볶는다."));
	}

	/** 소금을 "약간"으로 쓰는 응답. saltId가 null이면 냉장고에 없는 소금 */
	private static GeneratedRecipe aiRecipeWithSalt(Long saltId, Long... fridgeIds) {
		GeneratedRecipe base = aiRecipe(fridgeIds);
		List<GeneratedRecipe.Ingredient> ingredients = new java.util.ArrayList<>(base.ingredients());
		ingredients.add(new GeneratedRecipe.Ingredient(saltId, "소금", null, "TO_TASTE"));
		return new GeneratedRecipe(base.title(), base.cookMinutes(), base.tips(), ingredients, base.steps());
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
		excludeInSettings("EGG");
		Long egg = fridge("DAIRY_EGG", "계란", 1);
		Long onion = fridge("VEGETABLE", "양파", 10);
		fridge("MEAT", "상한 고기", -1);
		Long salt = fridge("SEASONING", "소금", 30);
		when(geminiRecipeAdapter.generate(any())).thenReturn(aiRecipeWithSalt(salt, egg, onion));

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
		assertThat(sent.excludedAllergenCodes()).as("빠른 생성도 설정의 제외 재료를 뺀다").containsExactly(AllergenCode.EGG);
		assertThat(sent.fridgeIngredients()).extracting(RecipeAiRequest.FridgeItem::ingredientId)
				.as("소비기한이 지난 재료는 보내지 않는다").containsExactly(egg, onion, salt);
		assertThat(sent.fridgeIngredients().get(0).expiringSoon()).isTrue();
		assertThat(sent.fridgeIngredients().get(1).expiringSoon()).isFalse();
		assertThat(recipeRows()).isEqualTo(1);
		assertThat(usageRows()).isEqualTo(1);
		assertThat(eventProperties("recipe_generated"))
				.contains("\"recipe_mode\": \"quick\"", "\"is_regenerated\": false", "\"expiring_used_count\": 1",
						"\"shortage_count\": 0", "\"remaining_quota\": 1");
	}

	@Test
	void 커스텀은_고른_재료와_인원과_옵션으로_만들고_선택값을_저장한다() throws Exception {
		Long egg = fridge("DAIRY_EGG", "계란", 5);
		Long onion = fridge("VEGETABLE", "양파", 5);
		fridge("MEAT", "돼지고기", 5);
		excludeInSettings("SHRIMP", "MILK");
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
		assertThat(sent.excludedAllergenCodes()).as("설정의 제외 재료에 고른 칩을 더하고 겹치면 한 번만")
				.containsExactly(AllergenCode.MILK, AllergenCode.SHRIMP);
		assertThat(jdbcTemplate.queryForObject("SELECT custom_options->>'taste' FROM recipes WHERE user_id = ?",
				String.class, userId)).isEqualTo("SPICY");
	}

	@Test
	void 커스텀은_인원이_필요하고_내_재료가_아니면_404이고_모르는_알레르기_코드는_400이다() throws Exception {
		Long othersEgg = createIngredient(createUser(), "DAIRY_EGG", "남의 계란", TODAY.plusDays(5));

		generate("{\"mode\":\"CUSTOM\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		generate("{\"mode\":\"CUSTOM\",\"servings\":2,\"ingredientIds\":[%d]}".formatted(othersEgg))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("INGREDIENT_NOT_FOUND"));
		generate("{\"mode\":\"MEAL_PLAN\"}")
				.andExpect(status().isBadRequest());
		// 알레르기는 ERD 부록 B 코드만 받는다. 모르는 값은 AI에 가지 않는다
		generate("{\"mode\":\"CUSTOM\",\"servings\":2,\"allergenCodes\":[\"NUTS\"]}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
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
		when(geminiRecipeAdapter.generate(any())).thenReturn(aiRecipeWithSalt(null));

		generate("{\"mode\":\"QUICK\"}").andExpect(status().isCreated());

		assertThat(sentRequest(1).supplementEnabled()).isTrue();
	}

	@Test
	void 만든_레시피의_재료_카테고리는_사전_냉장고_재료_AI_순서로_저장한다() throws Exception {
		jdbcTemplate.update("UPDATE users SET recipe_supplement_enabled = true WHERE id = ?", userId);
		Long egg = fridge("DAIRY_EGG", "계란", 3);
		Long mine = fridge("ETC", "우리집소스", 3);
		when(geminiRecipeAdapter.generate(any())).thenReturn(new GeneratedRecipe("볶음", 15, null, List.of(
				new GeneratedRecipe.Ingredient(egg, "계란", BigDecimal.ONE, "EA", "MEAT"),
				new GeneratedRecipe.Ingredient(mine, "우리집소스", BigDecimal.ONE, "TBSP", "SEASONING"),
				new GeneratedRecipe.Ingredient(null, "양파", BigDecimal.ONE, "EA", "FRUIT"),
				new GeneratedRecipe.Ingredient(null, "무지개채소", BigDecimal.ONE, "EA", "VEGETABLE")),
				List.of("볶는다.")));

		generate("{\"mode\":\"QUICK\"}").andExpect(status().isCreated());

		// 계란: 사전(DAIRY_EGG), 우리집소스: 사전에 없어 냉장고 재료의 ETC, 양파: AI의 FRUIT보다 사전(VEGETABLE), 무지개채소: AI 값
		assertThat(jdbcTemplate.queryForList(
				"SELECT name || ':' || category_code FROM recipe_ingredients ORDER BY sort_order", String.class))
				.containsExactly("계란:DAIRY_EGG", "우리집소스:ETC", "양파:VEGETABLE", "무지개채소:VEGETABLE");
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
				.contains("\"limit_feature\": \"recipe\"", "\"limit_total\": 2", "\"used_count\": 2");
	}

	@Test
	void AI를_기다리는_사이_다른_요청이_마지막_횟수를_쓰면_429이고_저장도_추가_차감도_하지_않는다() throws Exception {
		Long egg = fridge("DAIRY_EGG", "계란", 5);
		Long onion = fridge("VEGETABLE", "양파", 5);
		jdbcTemplate.update("INSERT INTO usage_logs (user_id, type, created_at) VALUES (?, 'RECIPE', ?)", userId,
				LocalDateTime.of(2026, 3, 4, 9, 0));
		when(geminiRecipeAdapter.generate(any())).thenAnswer(invocation -> {
			// 횟수 확인은 이미 통과했다. AI 응답을 기다리는 동안 동시 요청이 마지막 횟수를 쓴다.
			jdbcTemplate.update("INSERT INTO usage_logs (user_id, type, created_at) VALUES (?, 'RECIPE', ?)", userId,
					LocalDateTime.of(2026, 3, 4, 9, 30));
			return aiRecipe(egg, onion);
		});

		generate("{\"mode\":\"QUICK\"}")
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("GENERATION_LIMIT_EXCEEDED"))
				.andExpect(jsonPath("$.details.remaining").value(0));

		sentRequest(1);
		assertThat(recipeRows()).isZero();
		assertThat(usageRows()).isEqualTo(2);
		assertThat(eventProperties("quota_exceeded"))
				.contains("\"limit_feature\": \"recipe\"", "\"limit_total\": 2", "\"used_count\": 2");
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events WHERE event_name = 'quota_exceeded'",
				Integer.class)).isEqualTo(1);
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
