package com.remake.mealplan.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.remake.global.config.GeminiProperties;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.mealplan.application.port.out.GeneratedMealSlot;
import com.remake.mealplan.application.port.out.MealPlanAiRequest;
import com.remake.mealplan.domain.FridgeStock;
import com.remake.mealplan.domain.MealPurpose;
import com.remake.mealplan.domain.MealSchedule;
import com.remake.mealplan.domain.MealSlotKey;
import com.remake.mealplan.domain.MealType;
import com.remake.recipe.domain.RecipeUnit;
import com.remake.user.domain.AllergenCode;

import tools.jackson.databind.json.JsonMapper;

// Spring 없이 가짜 Gemini 서버(MockRestServiceServer)로 요청 모양과 응답·에러 변환을 확인한다. 실제 API는 부르지 않는다.
class GeminiMealPlanAdapterTest {

	private static final String BASE_URL = "http://gemini.test";
	private static final String ENDPOINT = BASE_URL + "/v1beta/models/test-model:generateContent";
	private static final JsonMapper JSON = JsonMapper.builder().build();
	private static final LocalDate START = LocalDate.of(2026, 3, 4);

	private MockRestServiceServer server;
	private GeminiMealPlanAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = adapterWithKey("test-key");
	}

	private GeminiMealPlanAdapter adapterWithKey(String apiKey) {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		RestClient restClient = builder.baseUrl(BASE_URL).build();
		return new GeminiMealPlanAdapter(restClient,
				new GeminiProperties(apiKey, "test-model", BASE_URL, Duration.ofSeconds(25)), JSON);
	}

	/** 재료 절약, 2인분, 2일 × 아침·저녁. 첫날 아침은 오늘 요리한 칸이라 남긴다. */
	private static MealPlanAiRequest request() {
		MealSchedule schedule = new MealSchedule(START, 2, Set.of(MealType.BREAKFAST, MealType.DINNER),
				Set.of(new MealSlotKey(START, MealType.BREAKFAST)));
		return new MealPlanAiRequest(MealPurpose.SAVING, 2, schedule, List.of(
				new FridgeStock(31L, "버섯", new BigDecimal("200"), IngredientUnit.G, START),
				new FridgeStock(33L, "돼지고기", new BigDecimal("300"), IngredientUnit.G, START.plusDays(7))),
				List.of(AllergenCode.SHRIMP));
	}

	/** Gemini 응답 모양: candidates[0].content.parts[0].text 에 식단표 JSON 문자열 */
	private static String geminiResponse(String json, String finishReason) {
		return JSON.writeValueAsString(Map.of("candidates", List.of(Map.of(
				"content", Map.of("role", "model", "parts", List.of(Map.of("text", json))),
				"finishReason", finishReason))));
	}

	private static final String PLAN_JSON = """
			{"slots": [
			  {"day": 1, "mealType": "DINNER", "title": "버섯 돼지고기 볶음", "cookMinutes": 20, "tips": null,
			   "ingredients": [
			     {"fridgeIngredientId": 31, "name": "버섯", "amount": 200, "unit": "G"},
			     {"fridgeIngredientId": 33, "name": "돼지고기", "amount": 150.12345, "unit": "g"},
			     {"fridgeIngredientId": null, "name": "후추", "amount": 3, "unit": "TO_TASTE"},
			     {"fridgeIngredientId": null, "name": "간장", "amount": 1, "unit": "SPOON"}],
			   "steps": ["버섯을 썬다.", "돼지고기와 볶는다."]},
			  {"day": 2, "mealType": "BREAKFAST", "title": "돼지고기 죽", "cookMinutes": 15,
			   "ingredients": [{"fridgeIngredientId": 33, "name": "돼지고기", "amount": 100, "unit": "G"}],
			   "steps": ["끓인다."]}]}
			""";

	@Test
	void 식단표_JSON을_칸으로_읽고_day를_날짜로_바꾼다() {
		server.expect(requestTo(ENDPOINT))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("x-goog-api-key", "test-key"))
				.andRespond(withSuccess(geminiResponse(PLAN_JSON, "STOP"), MediaType.APPLICATION_JSON));

		List<GeneratedMealSlot> slots = adapter.generate(request());

		server.verify();
		assertThat(slots).extracting(GeneratedMealSlot::key).containsExactly(
				new MealSlotKey(START, MealType.DINNER), new MealSlotKey(START.plusDays(1), MealType.BREAKFAST));
		GeneratedMealSlot dinner = slots.getFirst();
		assertThat(dinner.title()).isEqualTo("버섯 돼지고기 볶음");
		assertThat(dinner.cookMinutes()).isEqualTo(20);
		assertThat(dinner.tips()).isNull();
		assertThat(dinner.steps()).containsExactly("버섯을 썬다.", "돼지고기와 볶는다.");
		// 없는 단위(SPOON) 줄은 빼고, 소문자 단위는 맞추고, 양은 소수 셋째 자리로, "약간"은 양을 비운다
		assertThat(dinner.ingredients()).extracting(ingredient -> ingredient.name()).containsExactly("버섯", "돼지고기", "후추");
		assertThat(dinner.ingredients().get(1).unit()).isEqualTo(RecipeUnit.G);
		assertThat(dinner.ingredients().get(1).amount()).isEqualByComparingTo("150.123");
		assertThat(dinner.ingredients().get(2).amount()).isNull();
	}

	@Test
	void 보내는_데이터는_채울_칸과_며칠째까지_쓸_수_있는지와_제외_재료_코드다() {
		Map<String, Object> data = GeminiMealPlanAdapter.promptData(request());

		assertThat(data).containsEntry("purpose", "SAVING").containsEntry("servings", 2)
				.containsEntry("excludedAllergenCodes", List.of("SHRIMP"));
		// 남기는 칸(첫날 아침)은 빼고, 시작일이 1일째
		assertThat(data.get("slots")).isEqualTo(List.of(
				Map.of("day", 1L, "date", "2026-03-04", "mealType", "DINNER"),
				Map.of("day", 2L, "date", "2026-03-05", "mealType", "BREAKFAST"),
				Map.of("day", 2L, "date", "2026-03-05", "mealType", "DINNER")));
		// 소비기한은 날짜 대신 며칠째까지(기한 = 시작일이면 1, 7일 뒤면 8)
		assertThat(data.get("fridgeIngredients")).isEqualTo(List.of(
				Map.of("ingredientId", 31L, "name", "버섯", "amount", new BigDecimal("200"), "unit", "G",
						"usableUntilDay", 1L),
				Map.of("ingredientId", 33L, "name", "돼지고기", "amount", new BigDecimal("300"), "unit", "G",
						"usableUntilDay", 8L)));
	}

	@Test
	void 요청에는_JSON_응답_스키마와_고른_목적의_지시만_담고_키와_기한_날짜는_넣지_않는다() {
		server.expect(requestTo(ENDPOINT))
				.andExpect(content().string(allOf(
						containsString("\"responseMimeType\":\"application/json\""),
						containsString("\"responseSchema\""),
						containsString("장보기를 줄이는 식단"),
						containsString("끼니당 2개"),
						not(containsString("가벼운 식단")),
						containsString("usableUntilDay"),
						containsString("SHRIMP"),
						not(containsString("2026-03-11")),
						not(containsString("test-key")))))
				.andRespond(withSuccess(geminiResponse(PLAN_JSON, "STOP"), MediaType.APPLICATION_JSON));

		adapter.generate(request());

		server.verify();
	}

	@Test
	void 지시문에는_고른_목적_블록_하나만_들어간다() {
		for (MealPurpose purpose : MealPurpose.values()) {
			String instruction = MealPlanPrompt.systemInstruction(purpose);
			assertThat(instruction).contains(MealPlanPrompt.purposeBlock(purpose));
			Arrays.stream(MealPurpose.values()).filter(other -> other != purpose).forEach(other -> assertThat(instruction)
					.doesNotContain(MealPlanPrompt.purposeBlock(other).lines().findFirst().orElseThrow()));
			assertThat(instruction).contains("excludedAllergenCodes", "TO_TASTE", "조리 15분 안");
		}
	}

	@Test
	void 호출_한도_초과_429는_공급자_오류다() {
		server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
				.body("{\"error\":{\"message\":\"Resource has been exhausted\"}}"));

		assertError(ErrorCode.AI_UPSTREAM_ERROR);
	}

	@Test
	void 공급자_서버_오류는_공급자_오류이고_원래_문구를_내보내지_않는다() {
		server.expect(requestTo(ENDPOINT)).andRespond(withServerError().body("internal provider detail"));

		assertThatThrownBy(() -> adapter.generate(request()))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_UPSTREAM_ERROR);
					assertThat(e.getMessage()).doesNotContain("internal provider detail");
				});
	}

	@Test
	void 시간_초과는_AI_TIMEOUT이다() {
		server.expect(requestTo(ENDPOINT)).andRespond(withException(new SocketTimeoutException("Read timed out")));

		assertError(ErrorCode.AI_TIMEOUT);
	}

	@Test
	void 응답이_길이_제한으로_끊기면_형식_오류다() {
		server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(
				geminiResponse("{\"slots\": [{\"day\": 1", "MAX_TOKENS"), MediaType.APPLICATION_JSON));

		assertError(ErrorCode.AI_INVALID_RESPONSE);
	}

	@Test
	void JSON이_아닌_응답은_형식_오류다() {
		server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(
				geminiResponse("식단표를 만들 수 없어요", "STOP"), MediaType.APPLICATION_JSON));

		assertError(ErrorCode.AI_INVALID_RESPONSE);
	}

	@Test
	void 칸_목록이_없거나_칸_모양이_틀리면_형식_오류다() {
		for (String json : List.of(
				"{\"title\": \"한 끼\"}",
				"{\"slots\": [{\"day\": 1, \"mealType\": \"BRUNCH\", \"title\": \"a\", \"cookMinutes\": 5, \"ingredients\": [], \"steps\": []}]}",
				"{\"slots\": [{\"day\": \"1일\", \"mealType\": \"DINNER\", \"title\": \"a\", \"cookMinutes\": 5, \"ingredients\": [], \"steps\": []}]}",
				"{\"slots\": [{\"day\": 1, \"mealType\": \"DINNER\", \"title\": \"a\", \"cookMinutes\": \"5분\", \"ingredients\": [], \"steps\": []}]}",
				"{\"slots\": [{\"day\": 1, \"mealType\": \"DINNER\", \"title\": \"a\", \"cookMinutes\": 5, \"ingredients\": [{\"name\": \" \", \"amount\": 1, \"unit\": \"G\"}], \"steps\": []}]}",
				"{\"slots\": [{\"day\": 1, \"mealType\": \"DINNER\", \"title\": \"a\", \"cookMinutes\": 5, \"ingredients\": [{\"name\": \"파\", \"amount\": 0, \"unit\": \"G\"}], \"steps\": []}]}")) {
			assertThatThrownBy(() -> adapter.parse(json, START)).as(json)
					.isInstanceOfSatisfying(BusinessException.class,
							e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_INVALID_RESPONSE));
		}
	}

	@Test
	void API_키가_없으면_호출하지_않고_공급자_오류다() {
		GeminiMealPlanAdapter noKey = adapterWithKey("");

		assertThatThrownBy(() -> noKey.generate(request()))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_UPSTREAM_ERROR));
		server.verify();
	}

	private void assertError(ErrorCode expected) {
		assertThatThrownBy(() -> adapter.generate(request()))
				.isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(expected));
		server.verify();
	}
}
