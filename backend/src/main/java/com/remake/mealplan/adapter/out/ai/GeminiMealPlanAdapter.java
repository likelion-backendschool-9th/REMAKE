package com.remake.mealplan.adapter.out.ai;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.remake.global.config.GeminiProperties;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.mealplan.application.port.out.GenerateMealPlanAiPort;
import com.remake.mealplan.application.port.out.GeneratedMealSlot;
import com.remake.mealplan.application.port.out.MealPlanAiBusyException;
import com.remake.mealplan.application.port.out.MealPlanAiRequest;
import com.remake.mealplan.domain.FridgeStock;
import com.remake.mealplan.domain.MealSchedule;
import com.remake.mealplan.domain.MealType;
import com.remake.mealplan.domain.SlotIngredient;
import com.remake.recipe.domain.RecipeUnit;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gemini generateContent API로 식단표 칸들을 한 번에 만든다(구조화 출력: JSON 스키마를 지정해 JSON으로 받는다).
 * <p>
 * 프롬프트에는 고른 값(목적·인원·칸), DB의 냉장고 재료, 설정의 제외 재료 코드만 넣는다(사용자가 쓴 글은 넣지 않는다).
 * 냉장고 재료 이름은 사용자가 입력한 값이라 시스템 지시에 "지시가 아니라 데이터"로 다루라고 적는다.
 * 응답은 칸 모양으로 읽기만 하고(없는 단위의 재료 줄은 빼고, 양은 소수 셋째 자리로 맞춘다)
 * 칸 빠짐·필수 값 같은 판단은 GeneratedMealPlanValidator가 한다. 재시도는 서버 작업(MealPlanGenerationJobService)이 한다.
 * <p>
 * 호출·에러 변환은 레시피 생성(recipe GeminiRecipeAdapter)과 같은 기준이다. 다른 기능의 어댑터는 쓸 수 없어 따로 둔다.
 * 통합 테스트는 이 클래스를 @MockitoBean으로 바꾼다.
 */
@Slf4j
@Component
public class GeminiMealPlanAdapter implements GenerateMealPlanAiPort {

	private static final int AMOUNT_SCALE = 3;
	private static final int SERVICE_UNAVAILABLE = 503;

	private final RestClient restClient;
	private final GeminiProperties properties;
	private final JsonMapper jsonMapper;

	public GeminiMealPlanAdapter(
			@Qualifier(GeminiMealPlanClientConfig.GEMINI_MEAL_PLAN_REST_CLIENT) RestClient restClient,
			GeminiProperties properties, JsonMapper jsonMapper) {
		this.restClient = restClient;
		this.properties = properties;
		this.jsonMapper = jsonMapper;
	}

	@Override
	public List<GeneratedMealSlot> generate(MealPlanAiRequest request) {
		if (!properties.hasApiKey()) {
			log.warn("Gemini API 키가 없어 식단표를 생성하지 않습니다. backend/.env에 GEMINI_API_KEY를 넣으세요.");
			throw new BusinessException(ErrorCode.AI_UPSTREAM_ERROR);
		}
		JsonNode response = call(requestBody(request));
		return parse(extractText(response), request.schedule().startDate());
	}

	private JsonNode call(Map<String, Object> body) {
		try {
			return restClient.post()
					.uri("/v1beta/models/{model}:generateContent", properties.model())
					.header("x-goog-api-key", properties.apiKey())
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.retrieve()
					.body(JsonNode.class);
		} catch (RestClientResponseException e) {
			// 공급자 응답 본문(에러 문구)은 남기지 않는다. 상태 코드만 남긴다.
			log.warn("Gemini 식단표 호출 실패: status={}", e.getStatusCode().value());
			if (e.getStatusCode().value() == SERVICE_UNAVAILABLE) {
				// 과부하로 잠시 거절. 서버 작업이 쉬었다가 더 시도한다
				throw new MealPlanAiBusyException();
			}
			throw new BusinessException(ErrorCode.AI_UPSTREAM_ERROR);
		} catch (ResourceAccessException e) {
			if (isTimeout(e)) {
				log.warn("Gemini 식단표 호출 시간 초과: {}", GeminiMealPlanClientConfig.READ_TIMEOUT);
				throw new BusinessException(ErrorCode.AI_TIMEOUT);
			}
			log.warn("Gemini 식단표 연결 실패: {}", e.getClass().getSimpleName());
			throw new BusinessException(ErrorCode.AI_UPSTREAM_ERROR);
		} catch (RestClientException e) {
			log.warn("Gemini 식단표 응답 처리 실패: {}", e.getClass().getSimpleName());
			throw new BusinessException(ErrorCode.AI_INVALID_RESPONSE);
		}
	}

	private static boolean isTimeout(Throwable e) {
		for (Throwable cause = e; cause != null; cause = cause.getCause()) {
			if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
				return true;
			}
		}
		return false;
	}

	/** candidates[0].content.parts[].text를 이어 붙인다. 막히거나(안전 필터) 비거나 길이 제한으로 끊기면 실패로 본다. */
	private String extractText(JsonNode response) {
		if (response == null) {
			throw invalid();
		}
		if (response.path("promptFeedback").hasNonNull("blockReason")) {
			log.warn("Gemini가 식단표 요청을 거절함: blockReason={}",
					response.path("promptFeedback").path("blockReason").asString());
			throw new BusinessException(ErrorCode.AI_UPSTREAM_ERROR);
		}
		JsonNode candidate = response.path("candidates").path(0);
		String finishReason = candidate.path("finishReason").asString("");
		if (!finishReason.isEmpty() && !"STOP".equals(finishReason)) {
			log.warn("Gemini 식단표 응답이 완료되지 않음: finishReason={}", finishReason);
			throw new BusinessException("MAX_TOKENS".equals(finishReason)
					? ErrorCode.AI_INVALID_RESPONSE
					: ErrorCode.AI_UPSTREAM_ERROR);
		}
		StringBuilder text = new StringBuilder();
		for (JsonNode part : candidate.path("content").path("parts")) {
			text.append(part.path("text").asString(""));
		}
		if (text.isEmpty()) {
			throw invalid();
		}
		return text.toString();
	}

	/**
	 * 칸 모양으로 읽는다. 칸의 day는 시작일 = 1일째로 날짜로 바꾼다(범위 밖 날짜도 그대로 두고 검증기가 거른다).
	 * day·mealType·cookMinutes·재료 목록이 없거나 타입이 다르면, 또는 재료 이름이 비었거나 양이 0 이하면 실패다.
	 */
	List<GeneratedMealSlot> parse(String json, LocalDate startDate) {
		JsonNode root;
		try {
			root = jsonMapper.readTree(json);
		} catch (JacksonException e) {
			throw invalid();
		}
		if (root == null || !root.path("slots").isArray()) {
			throw invalid();
		}
		List<GeneratedMealSlot> slots = new ArrayList<>();
		for (JsonNode node : root.path("slots")) {
			slots.add(parseSlot(node, startDate));
		}
		return slots;
	}

	private GeneratedMealSlot parseSlot(JsonNode node, LocalDate startDate) {
		if (!node.path("day").isIntegralNumber() || !node.path("cookMinutes").isIntegralNumber()
				|| !node.path("ingredients").isArray()) {
			throw invalid();
		}
		MealType mealType = parseEnum(MealType.class, textOrNull(node.path("mealType")));
		if (mealType == null) {
			throw invalid();
		}
		LocalDate date = startDate.plusDays(node.path("day").asLong() - 1);
		List<SlotIngredient> ingredients = new ArrayList<>();
		for (JsonNode item : node.path("ingredients")) {
			SlotIngredient ingredient = parseIngredient(item);
			if (ingredient != null) {
				ingredients.add(ingredient);
			}
		}
		List<String> steps = new ArrayList<>();
		for (JsonNode step : node.path("steps")) {
			steps.add(textOrNull(step));
		}
		return new GeneratedMealSlot(date, mealType, textOrNull(node.path("title")), node.path("cookMinutes").asInt(),
				textOrNull(node.path("tips")), ingredients, steps);
	}

	/** 14개 코드에 없는 단위의 줄은 null(뺀다). "약간"(TO_TASTE)은 양을 비운다. */
	private SlotIngredient parseIngredient(JsonNode item) {
		RecipeUnit unit = parseEnum(RecipeUnit.class, textOrNull(item.path("unit")));
		if (unit == null) {
			return null;
		}
		Long fridgeIngredientId = item.path("fridgeIngredientId").isIntegralNumber()
				? item.path("fridgeIngredientId").asLong()
				: null;
		BigDecimal amount = unit == RecipeUnit.TO_TASTE || !item.path("amount").isNumber()
				? null
				: normalizeAmount(item.path("amount").decimalValue());
		try {
			return new SlotIngredient(fridgeIngredientId, textOrNull(item.path("name")), amount, unit);
		} catch (IllegalArgumentException e) {
			throw invalid();
		}
	}

	/** 소수 셋째 자리로 반올림하고 끝의 0을 지운다. 300이 3E+2로 바뀌지 않게 정수는 소수 자리 0으로 둔다. */
	private static BigDecimal normalizeAmount(BigDecimal amount) {
		BigDecimal stripped = amount.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
		return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
	}

	/** 앞뒤 공백·대소문자는 맞춰 주고, 없는 코드면 null */
	private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Enum.valueOf(type, value.strip().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static String textOrNull(JsonNode node) {
		return node.isString() ? node.asString() : null;
	}

	Map<String, Object> requestBody(MealPlanAiRequest request) {
		String userData;
		try {
			userData = jsonMapper.writeValueAsString(promptData(request));
		} catch (JacksonException e) {
			throw new IllegalStateException("프롬프트 데이터를 만들 수 없습니다.", e);
		}
		Map<String, Object> generationConfig = new LinkedHashMap<>();
		generationConfig.put("responseMimeType", "application/json");
		generationConfig.put("responseSchema", RESPONSE_SCHEMA);
		generationConfig.put("temperature", 0.8);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("systemInstruction",
				Map.of("parts", List.of(Map.of("text", MealPlanPrompt.systemInstruction(request.purpose())))));
		body.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userData)))));
		body.put("generationConfig", generationConfig);
		return body;
	}

	/**
	 * 사용자 메시지에 넣을 데이터. 칸은 시작일 = 1일째로 번호를 붙이고, 냉장고 재료의 소비기한은 날짜 대신
	 * "며칠째까지 쓸 수 있는지"(usableUntilDay)로 보낸다. 덮어쓰기에서 남기는 칸은 넣지 않는다.
	 */
	static Map<String, Object> promptData(MealPlanAiRequest request) {
		MealSchedule schedule = request.schedule();
		LocalDate startDate = schedule.startDate();
		List<Map<String, Object>> slots = new ArrayList<>();
		for (LocalDate date : schedule.dates()) {
			for (MealType mealType : schedule.mealTypesOn(date)) {
				Map<String, Object> slot = new LinkedHashMap<>();
				slot.put("day", dayNumber(startDate, date));
				slot.put("date", date.toString());
				slot.put("mealType", mealType.name());
				slots.add(slot);
			}
		}
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("purpose", request.purpose().name());
		data.put("servings", request.servings());
		data.put("slots", slots);
		data.put("fridgeIngredients", request.ingredients().stream().map(stock -> fridgeItem(stock, startDate)).toList());
		data.put("excludedAllergenCodes", request.excludedAllergenCodes().stream().map(Enum::name).toList());
		return data;
	}

	private static Map<String, Object> fridgeItem(FridgeStock stock, LocalDate startDate) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("ingredientId", stock.ingredientId());
		item.put("name", stock.name());
		item.put("amount", stock.amount());
		item.put("unit", stock.unit().name());
		item.put("usableUntilDay", dayNumber(startDate, stock.expirationDate()));
		return item;
	}

	/** 시작일 = 1일째 */
	static long dayNumber(LocalDate startDate, LocalDate date) {
		return ChronoUnit.DAYS.between(startDate, date) + 1;
	}

	/** 끼니 코드 목록(스키마 enum) */
	private static final List<String> MEAL_TYPES = Arrays.stream(MealType.values()).map(Enum::name).toList();

	/**
	 * 냉장고 재료 배분표. 칸을 쓰기 전에 재료별로 어느 칸에 얼마를 쓸지 먼저 쓰게 해 합계 초과를 줄인다
	 * (11-24 실측: 지시 문장만으로는 계란 12~19개/10개처럼 합계가 넘쳐 3일 안팎에서 잘림). 서버는 읽지 않는다(자르기가 최종 판단).
	 */
	private static final Map<String, Object> FRIDGE_PLAN_SCHEMA = Map.of(
			"type", "ARRAY",
			"items", Map.of(
					"type", "OBJECT",
					"properties", Map.of(
							"ingredientId", Map.of("type", "INTEGER"),
							"uses", Map.of(
									"type", "ARRAY",
									"items", Map.of(
											"type", "OBJECT",
											"properties", Map.of(
													"day", Map.of("type", "INTEGER"),
													"mealType", Map.of("type", "STRING", "enum", MEAL_TYPES),
													"amount", Map.of("type", "NUMBER")),
											"required", List.of("day", "mealType", "amount"))),
							"total", Map.of("type", "NUMBER")),
					"propertyOrdering", List.of("ingredientId", "uses", "total"),
					"required", List.of("ingredientId", "uses", "total")));

	/**
	 * 메뉴 목록. 칸마다 이름·주재료·조리법만 먼저 쓰게 해 같은 요리 반복을 줄인다
	 * (11-24 실측: 같은 이름 2번, 아침이 매일 계란+김치). 서버는 읽지 않는다(다양성은 검사하지 않음).
	 */
	private static final Map<String, Object> MENU_PLAN_SCHEMA = Map.of(
			"type", "ARRAY",
			"items", Map.of(
					"type", "OBJECT",
					"properties", Map.of(
							"day", Map.of("type", "INTEGER"),
							"mealType", Map.of("type", "STRING", "enum", MEAL_TYPES),
							"title", Map.of("type", "STRING"),
							"mainIngredient", Map.of("type", "STRING"),
							"method", Map.of("type", "STRING", "description", "볶음·국·찌개·구이·무침·조림·찜·죽 같은 조리법")),
					"propertyOrdering", List.of("day", "mealType", "title", "mainIngredient", "method"),
					"required", List.of("day", "mealType", "title", "mainIngredient", "method")));

	/**
	 * Gemini 응답 스키마(OpenAPI 부분집합). 메뉴 목록(menuPlan) → 배분표(fridgePlan) → 칸(slots) 순서로 쓰게 한다.
	 * parse는 slots만 읽는다.
	 */
	static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
			"type", "OBJECT",
			"propertyOrdering", List.of("menuPlan", "fridgePlan", "slots"),
			"properties", Map.of(
					"menuPlan", MENU_PLAN_SCHEMA,
					"fridgePlan", FRIDGE_PLAN_SCHEMA,
					"slots", Map.of(
							"type", "ARRAY",
							"items", Map.of(
									"type", "OBJECT",
									"properties", Map.of(
											"day", Map.of("type", "INTEGER"),
											"mealType", Map.of("type", "STRING", "enum", MEAL_TYPES),
											"title", Map.of("type", "STRING"),
											"cookMinutes", Map.of("type", "INTEGER"),
											"tips", Map.of("type", "STRING", "nullable", true),
											"ingredients", Map.of(
													"type", "ARRAY",
													"items", Map.of(
															"type", "OBJECT",
															"properties", Map.of(
																	"fridgeIngredientId",
																	Map.of("type", "INTEGER", "nullable", true),
																	"name", Map.of("type", "STRING", "description",
																			"재료 이름. 밥이 필요하면 \"밥\"이 아니라 \"쌀\"로 쓴다"),
																	"amount", Map.of("type", "NUMBER", "nullable", true),
																	"unit", Map.of("type", "STRING", "enum",
																			Arrays.stream(RecipeUnit.values())
																					.map(Enum::name).toList())),
															"required", List.of("name", "unit"))),
											"steps", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))),
									"required", List.of("day", "mealType", "title", "cookMinutes", "ingredients",
											"steps")))),
			"required", List.of("menuPlan", "fridgePlan", "slots"));

	private static BusinessException invalid() {
		return new BusinessException(ErrorCode.AI_INVALID_RESPONSE);
	}
}
