package com.remake.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import com.remake.support.IntegrationTestSupport;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// OpenAPI 문서와 로컬 CORS 설정을 확인한다.
class WebConfigTest extends IntegrationTestSupport {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@Test
	void OpenAPI_문서를_제공한다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Remake API"))
				.andExpect(jsonPath("$.components.securitySchemes.cookieAuth.type").value("apiKey"))
				.andExpect(jsonPath("$.components.securitySchemes.cookieAuth.in").value("cookie"))
				.andExpect(jsonPath("$.components.securitySchemes.cookieAuth.name").value("ACCESS_TOKEN"));
	}

	// 이슈 #182: 프론트 타입 생성이 기대는 OpenAPI 계약이 깨지지 않는지 실제 /v3/api-docs로 확인한다.
	@Test
	void OpenAPI_계약_규칙을_지킨다() throws Exception {
		String body = mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString(StandardCharsets.UTF_8);
		exportIfRequested(body);
		JsonNode root = JSON.readTree(body);
		JsonNode schemas = root.path("components").path("schemas");
		JsonNode paths = root.path("paths");

		// 0) /api/v1/** 경로만 문서에 포함된다(테스트 전용 컨트롤러 제외).
		assertThat(paths.propertyNames()).isNotEmpty().allSatisfy(path -> assertThat(path).startsWith("/api/v1/"));

		// 1) 충돌하기 쉬운 중첩 타입 이름은 고유 이름으로 바뀌어 있어야 한다.
		for (String name : List.of("Item", "Ingredient", "Step")) {
			assertThat(schemas.has(name)).as("충돌 이름 스키마 %s", name).isFalse();
		}
		for (String name : List.of("CookPreviewItem", "CookRequestItem", "RecipeDetailIngredient", "RecipeDetailStep",
				"SharedRecipeIngredient", "SharedRecipeStep", "SharedRecipeRankingItem", "ReceiptCreateItem",
				"ReceiptDetailItem", "ExpenseDailyItem")) {
			assertThat(schemas.has(name)).as("고유 이름 스키마 %s", name).isTrue();
		}

		// 2) 공통 오류 응답 스키마와 operation의 default 응답 연결
		assertThat(schemas.has("ErrorResponse")).as("ErrorResponse 스키마").isTrue();
		assertThat(schemas.has("FieldError")).as("FieldError 스키마").isTrue();
		for (String[] target : new String[][] {
				{"/api/v1/user/recipes/generate", "post"},
				{"/api/v1/user/recipes/{id}", "get"},
				{"/api/v1/user/events", "post"},
				{"/api/v1/user/account/deletion", "post"}}) {
			JsonNode defaultResponse = paths.path(target[0]).path(target[1]).path("responses").path("default");
			assertThat(defaultResponse.path("content").path("application/json").path("schema").path("$ref").asString())
					.as("%s %s default 응답", target[1], target[0])
					.isEqualTo("#/components/schemas/ErrorResponse");
		}

		// 3) 계정 삭제 요청은 204를 문서에 가진다.
		assertThat(paths.path("/api/v1/user/account/deletion").path("post").path("responses").has("204"))
				.as("계정 삭제 204 응답").isTrue();

		// 4) YearMonth 필드는 객체가 아니라 string으로 보인다.
		assertThat(isString(schemas.path("MonthlyPattern").path("properties").path("month")))
				.as("MonthlyPattern.month 타입").isTrue();
		assertThat(isString(schemas.path("MonthlyReceipts").path("properties").path("month")))
				.as("MonthlyReceipts.month 타입").isTrue();

		// 5) 순수 응답 스키마의 non-null 필드는 required, nullable 필드는 required가 아니다.
		assertThat(requiredNames(schemas.path("RecipeDetailResponse")))
				.contains("id", "title", "servings", "cookMinutes", "ingredients", "steps", "saved", "favorite",
						"shareable")
				.doesNotContain("tips");
		assertThat(requiredNames(schemas.path("CookPreviewResponse"))).contains("items");
		assertThat(requiredNames(schemas.path("CookPreviewItem")))
				.contains("ingredientId", "name", "unit")
				.doesNotContain("amount");

		// 6) 공통 헤더는 operation마다 한 번씩만, 선택 항목으로 선언된다.
		List<String> headers = List.of("X-Client-Platform", "X-App-Version", "X-Screen");
		JsonNode eventParameters = paths.path("/api/v1/user/events").path("post").path("parameters");
		JsonNode generateParameters = paths.path("/api/v1/user/recipes/generate").path("post").path("parameters");
		for (String header : headers) {
			assertThat(headerParameters(eventParameters, header)).as("events %s 개수", header).hasSize(1);
			List<JsonNode> generateHeaders = headerParameters(generateParameters, header);
			assertThat(generateHeaders).as("generate %s 개수", header).hasSize(1);
			assertThat(generateHeaders.get(0).path("required").asBoolean(false))
					.as("generate %s required", header).isFalse();
		}

		// 7) 구독 상태 enum
		JsonNode statusEnum = schemas.path("SubscriptionResponse").path("properties").path("status").path("enum");
		assertThat(statusEnum.valueStream().map(JsonNode::asString).toList())
				.containsExactly("NONE", "ACTIVE", "CANCELED");
	}

	// OPENAPI_EXPORT_PATH가 있을 때만 문서 원문을 파일로 저장한다. 평소 테스트·CI에서는 쓰지 않는다.
	private static void exportIfRequested(String body) throws IOException {
		String exportPath = System.getenv("OPENAPI_EXPORT_PATH");
		if (exportPath == null || exportPath.isBlank()) {
			return;
		}
		Path target = Path.of(exportPath);
		if (target.getParent() != null) {
			Files.createDirectories(target.getParent());
		}
		Files.writeString(target, body, StandardCharsets.UTF_8);
	}

	// OpenAPI 3.0(type: "string")과 3.1(type: ["string", ...])을 모두 받아들인다.
	private static boolean isString(JsonNode schema) {
		JsonNode type = schema.path("type");
		if (type.isString()) {
			return "string".equals(type.asString());
		}
		return type.isArray() && type.valueStream().anyMatch(t -> "string".equals(t.asString()));
	}

	private static List<String> requiredNames(JsonNode schema) {
		return schema.path("required").valueStream().map(JsonNode::asString).toList();
	}

	private static List<JsonNode> headerParameters(JsonNode parameters, String name) {
		return parameters.valueStream()
				.filter(p -> "header".equals(p.path("in").asString()) && name.equalsIgnoreCase(p.path("name").asString()))
				.toList();
	}

	@Test
	void 허용한_프론트_주소의_사전_요청은_통과한다() throws Exception {
		mockMvc.perform(options("/api/v1/user/anything")
						.header(HttpHeaders.ORIGIN, "http://localhost:3000")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
	}

	@Test
	void 허용하지_않은_주소의_사전_요청은_막는다() throws Exception {
		mockMvc.perform(options("/api/v1/user/anything")
						.header(HttpHeaders.ORIGIN, "http://evil.example")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(status().isForbidden());
	}
}
