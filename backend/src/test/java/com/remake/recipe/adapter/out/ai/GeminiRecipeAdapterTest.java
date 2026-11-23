package com.remake.recipe.adapter.out.ai;

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
import java.util.List;
import java.util.Map;

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
import com.remake.recipe.application.port.out.GeneratedRecipe;
import com.remake.recipe.application.port.out.RecipeAiRequest;
import com.remake.recipe.domain.CustomOptions;
import com.remake.recipe.domain.RecipeSource;
import com.remake.recipe.domain.Taste;

import tools.jackson.databind.json.JsonMapper;

// Spring 없이 가짜 Gemini 서버(MockRestServiceServer)로 요청 모양과 응답·에러 변환을 확인한다. 실제 API는 부르지 않는다.
class GeminiRecipeAdapterTest {

	private static final String BASE_URL = "http://gemini.test";
	private static final String ENDPOINT = BASE_URL + "/v1beta/models/test-model:generateContent";
	private static final JsonMapper JSON = JsonMapper.builder().build();

	private MockRestServiceServer server;
	private GeminiRecipeAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = adapterWithKey("test-key");
	}

	private GeminiRecipeAdapter adapterWithKey(String apiKey) {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		RestClient restClient = builder.baseUrl(BASE_URL).build();
		return new GeminiRecipeAdapter(restClient,
				new GeminiProperties(apiKey, "test-model", BASE_URL, Duration.ofSeconds(25)), JSON);
	}

	private static RecipeAiRequest request() {
		return new RecipeAiRequest(RecipeSource.CUSTOM, 2, false,
				List.of(new RecipeAiRequest.FridgeItem(10L, "계란", new BigDecimal("6"), "EA", LocalDate.of(2026, 3, 6),
						true)),
				new CustomOptions(2, List.of(10L), Taste.SPICY, null, null, null, null, List.of("SHRIMP")),
				List.of("SHRIMP"));
	}

	/** Gemini 응답 모양: candidates[0].content.parts[0].text 에 레시피 JSON 문자열 */
	private static String geminiResponse(String recipeJson, String finishReason) {
		return JSON.writeValueAsString(Map.of("candidates", List.of(Map.of(
				"content", Map.of("role", "model", "parts", List.of(Map.of("text", recipeJson))),
				"finishReason", finishReason))));
	}

	private static final String RECIPE_JSON = """
			{"title": "매운 계란말이", "cookMinutes": 15, "tips": "약불로 익혀요.",
			 "ingredients": [
			   {"fridgeIngredientId": 10, "name": "계란", "amount": 3, "unit": "EA"},
			   {"fridgeIngredientId": null, "name": "소금", "amount": null, "unit": "TO_TASTE"}],
			 "steps": ["계란을 푼다.", "팬에 말아 익힌다."]}
			""";

	@Test
	void 레시피_JSON을_읽어_돌려준다() {
		server.expect(requestTo(ENDPOINT))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("x-goog-api-key", "test-key"))
				.andRespond(withSuccess(geminiResponse(RECIPE_JSON, "STOP"), MediaType.APPLICATION_JSON));

		GeneratedRecipe recipe = adapter.generate(request());

		server.verify();
		assertThat(recipe.title()).isEqualTo("매운 계란말이");
		assertThat(recipe.cookMinutes()).isEqualTo(15);
		assertThat(recipe.tips()).isEqualTo("약불로 익혀요.");
		assertThat(recipe.ingredients()).hasSize(2);
		assertThat(recipe.ingredients().get(0).fridgeIngredientId()).isEqualTo(10L);
		assertThat(recipe.ingredients().get(0).amount()).isEqualByComparingTo("3");
		assertThat(recipe.ingredients().get(0).unit()).isEqualTo("EA");
		assertThat(recipe.ingredients().get(1).fridgeIngredientId()).isNull();
		assertThat(recipe.ingredients().get(1).amount()).isNull();
		assertThat(recipe.steps()).containsExactly("계란을 푼다.", "팬에 말아 익힌다.");
	}

	@Test
	void 요청에는_JSON_응답_스키마와_옵션_코드와_냉장고_재료만_담는다() {
		server.expect(requestTo(ENDPOINT))
				.andExpect(content().string(allOf(
						containsString("\"responseMimeType\":\"application/json\""),
						containsString("\"responseSchema\""),
						containsString("systemInstruction"),
						// 사용자 메시지(JSON 문자열) 안의 값
						containsString("계란"),
						containsString("SPICY"),
						containsString("SHRIMP"),
						containsString("supplementEnabled"),
						containsString("expiringSoon"),
						// API 키는 헤더로만 보내고 본문에 넣지 않는다
						not(containsString("test-key")))))
				.andRespond(withSuccess(geminiResponse(RECIPE_JSON, "STOP"), MediaType.APPLICATION_JSON));

		adapter.generate(request());

		server.verify();
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
	void 안전_필터로_요청이_막히면_공급자_오류다() {
		server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(
				"{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}", MediaType.APPLICATION_JSON));

		assertError(ErrorCode.AI_UPSTREAM_ERROR);
	}

	@Test
	void 응답이_길이_제한으로_끊기면_형식_오류다() {
		server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(
				geminiResponse("{\"title\": \"매운", "MAX_TOKENS"), MediaType.APPLICATION_JSON));

		assertError(ErrorCode.AI_INVALID_RESPONSE);
	}

	@Test
	void JSON이_아닌_응답은_형식_오류다() {
		server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(
				geminiResponse("레시피를 만들 수 없어요", "STOP"), MediaType.APPLICATION_JSON));

		assertError(ErrorCode.AI_INVALID_RESPONSE);
	}

	@Test
	void 후보가_없으면_형식_오류다() {
		server.expect(requestTo(ENDPOINT)).andRespond(withSuccess("{\"candidates\":[]}", MediaType.APPLICATION_JSON));

		assertError(ErrorCode.AI_INVALID_RESPONSE);
	}

	@Test
	void 빠지거나_타입이_다른_값은_null로_읽고_판단은_검증기에_맡긴다() {
		GeneratedRecipe recipe = adapter.parse("""
				{"title": 123, "cookMinutes": "15분", "ingredients": [{"name": "계란", "amount": "많이", "unit": 1}]}
				""");

		assertThat(recipe.title()).isNull();
		assertThat(recipe.cookMinutes()).isNull();
		assertThat(recipe.steps()).isNull();
		assertThat(recipe.ingredients()).singleElement().satisfies(ingredient -> {
			assertThat(ingredient.name()).isEqualTo("계란");
			assertThat(ingredient.amount()).isNull();
			assertThat(ingredient.unit()).isNull();
		});
	}

	@Test
	void API_키가_없으면_호출하지_않고_공급자_오류다() {
		GeminiRecipeAdapter noKey = adapterWithKey("");

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
