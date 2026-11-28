package com.remake.global.config;

import java.time.YearMonth;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.remake.global.error.ErrorResponse;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenAPI 문서 설정. 문서는 /v3/api-docs, 화면은 /swagger-ui.html.
 * 프론트는 로컬 백엔드의 /v3/api-docs로 API 클라이언트를 만든다(frontend의 pnpm gen:api).
 * 운영 프록시는 /api/만 백엔드로 보내므로 이 주소들은 운영 도메인에서 열리지 않는다.
 * 여기서 바꾸는 것은 문서뿐이고, API 동작과 JSON 직렬화는 그대로다.
 */
@Configuration
public class OpenApiConfig {

	/** 로그인이 필요한 API에 붙일 쿠키 인증 스킴 이름(#42). */
	public static final String COOKIE_AUTH = "cookieAuth";

	private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";
	private static final String ERROR_RESPONSE_SCHEMA = "ErrorResponse";
	private static final String API_PATH_PREFIX = "/api/v1/";
	private static final String NULL_TYPE = "null";

	static {
		// YearMonth는 Jackson이 "2026-11" 문자열로 주고받는다. 문서에서도 객체 대신 string으로 보이게 한다(응답 필드·@RequestParam 공통).
		SpringDocUtils.getConfig().replaceWithSchema(YearMonth.class, stringSchema().example("2026-11"));
	}

	@Bean
	public OpenAPI remakeOpenApi(AuthProperties authProperties) {
		return new OpenAPI()
				.info(new Info().title("Remake API").version("v1"))
				.components(new Components().addSecuritySchemes(COOKIE_AUTH,
						new SecurityScheme()
								.type(SecurityScheme.Type.APIKEY)
								.in(SecurityScheme.In.COOKIE)
								.name(authProperties.accessCookieName())));
	}

	/**
	 * 생성된 문서를 프론트 타입 생성(openapi-typescript)에 맞게 보정한다.
	 * 1) ErrorResponse 스키마를 등록하고 모든 operation에 default 오류 응답으로 연결한다.
	 * 2) /api/v1/** operation에 공통 헤더(X-Client-Platform, X-App-Version, X-Screen)를 선택 항목으로 선언한다.
	 * 3) 순수 응답 스키마의 non-null 필드를 required로 표시한다.
	 */
	@Bean
	public OpenApiCustomizer remakeOpenApiCustomizer() {
		return openApi -> {
			registerErrorSchemas(openApi);
			if (openApi.getPaths() == null) {
				return;
			}
			openApi.getPaths().forEach((path, pathItem) -> {
				for (Operation operation : pathItem.readOperations()) {
					addDefaultErrorResponse(operation);
					if (path.startsWith(API_PATH_PREFIX)) {
						addCommonHeaders(operation);
					}
				}
			});
			markResponseFieldsRequired(openApi);
		};
	}

	// ---- 1) 공통 오류 응답 ----

	/** ErrorResponse와 FieldError 스키마가 components.schemas에 없으면 추가한다. */
	@SuppressWarnings("rawtypes")
	private static void registerErrorSchemas(OpenAPI openApi) {
		if (openApi.getComponents() == null) {
			openApi.components(new Components());
		}
		boolean openapi31 = isOpenApi31(openApi);
		Map<String, Schema> schemas = ModelConverters.getInstance(openapi31)
				.readAll(new AnnotatedType(ErrorResponse.class));
		Components components = openApi.getComponents();
		schemas.forEach((name, schema) -> {
			if (components.getSchemas() == null || !components.getSchemas().containsKey(name)) {
				components.addSchemas(name, schema);
			}
		});
	}

	/** 이미 default 응답이 있으면 그대로 두고, 없으면 ErrorResponse를 가리키는 default 응답을 붙인다. */
	private static void addDefaultErrorResponse(Operation operation) {
		if (operation.getResponses() == null || operation.getResponses().containsKey("default")) {
			return;
		}
		operation.getResponses().addApiResponse("default", new ApiResponse()
				.description("오류 응답")
				.content(new Content().addMediaType("application/json",
						new MediaType().schema(new Schema<>().$ref(SCHEMA_REF_PREFIX + ERROR_RESPONSE_SCHEMA)))));
	}

	// ---- 2) 공통 헤더 ----

	private record CommonHeader(String name, String description, List<String> allowedValues) {
	}

	private static final List<CommonHeader> COMMON_HEADERS = List.of(
			new CommonHeader("X-Client-Platform", "클라이언트 플랫폼", List.of("TWA", "PWA", "WEB")),
			new CommonHeader("X-App-Version", "앱 버전", List.of()),
			new CommonHeader("X-Screen", "현재 화면 코드", List.of()));

	/** 같은 이름의 header 파라미터가 이미 있으면(대소문자 무시) 추가하지 않고 비어 있는 스키마·설명만 채운다. */
	private static void addCommonHeaders(Operation operation) {
		for (CommonHeader header : COMMON_HEADERS) {
			Parameter existing = findHeader(operation, header.name());
			if (existing == null) {
				operation.addParametersItem(new HeaderParameter()
						.name(header.name())
						.required(false)
						.description(header.description())
						.schema(headerSchema(header)));
				continue;
			}
			if (existing.getDescription() == null || existing.getDescription().isBlank()) {
				existing.setDescription(header.description());
			}
			if (existing.getSchema() == null) {
				existing.setSchema(headerSchema(header));
			} else if (!header.allowedValues().isEmpty()) {
				fillEnumIfMissing(existing.getSchema(), header.allowedValues());
			}
		}
	}

	/** 기존 헤더 스키마가 string이고 enum이 비어 있을 때만 허용값을 채운다. */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void fillEnumIfMissing(Schema schema, List<String> allowedValues) {
		boolean isString = "string".equals(schema.getType())
				|| (schema.getTypes() != null && schema.getTypes().contains("string"));
		if (isString && (schema.getEnum() == null || schema.getEnum().isEmpty())) {
			schema.setEnum(new ArrayList<>(allowedValues));
		}
	}

	private static Parameter findHeader(Operation operation, String name) {
		if (operation.getParameters() == null) {
			return null;
		}
		for (Parameter parameter : operation.getParameters()) {
			if ("header".equalsIgnoreCase(parameter.getIn()) && name.equalsIgnoreCase(parameter.getName())) {
				return parameter;
			}
		}
		return null;
	}

	private static Schema<String> headerSchema(CommonHeader header) {
		StringSchema schema = stringSchema();
		if (!header.allowedValues().isEmpty()) {
			schema.setEnum(header.allowedValues());
		}
		return schema;
	}

	// ---- 3) 응답 스키마 required 보정 ----

	/**
	 * requestBody에서 닿는 스키마와 2xx 응답에서 닿는 스키마를 $ref를 따라 모은 뒤,
	 * 요청에 쓰이지 않는 응답 전용 스키마의 nullable이 아닌 필드를 required에 넣는다.
	 * 요청 DTO의 선택 필드는 건드리지 않는다.
	 */
	@SuppressWarnings("rawtypes")
	private static void markResponseFieldsRequired(OpenAPI openApi) {
		Map<String, Schema> components = openApi.getComponents() == null ? null
				: openApi.getComponents().getSchemas();
		if (components == null || components.isEmpty()) {
			return;
		}
		Set<String> requestSchemas = new HashSet<>();
		Set<String> responseSchemas = new HashSet<>();
		for (PathItem pathItem : openApi.getPaths().values()) {
			for (Operation operation : pathItem.readOperations()) {
				if (operation.getRequestBody() != null) {
					collectFromContent(operation.getRequestBody().getContent(), components, requestSchemas);
				}
				if (operation.getResponses() != null) {
					operation.getResponses().forEach((status, response) -> {
						if (status.startsWith("2") || "default".equals(status)) {
							collectFromContent(response.getContent(), components, responseSchemas);
						}
					});
				}
			}
		}
		responseSchemas.removeAll(requestSchemas);
		for (String name : responseSchemas) {
			Schema schema = components.get(name);
			if (schema == null || schema.getProperties() == null) {
				continue;
			}
			Map<String, Schema> properties = schema.getProperties();
			properties.forEach((propertyName, property) -> {
				boolean alreadyRequired = schema.getRequired() != null && schema.getRequired().contains(propertyName);
				if (!alreadyRequired && !isNullable(property)) {
					schema.addRequiredItem(propertyName);
				}
			});
		}
	}

	@SuppressWarnings("rawtypes")
	private static void collectFromContent(Content content, Map<String, Schema> components, Set<String> names) {
		if (content == null) {
			return;
		}
		for (MediaType mediaType : content.values()) {
			collectRefs(mediaType.getSchema(), components, names);
		}
	}

	/** 스키마 트리를 돌며 $ref로 가리키는 컴포넌트 이름을 모은다. 한 번 본 컴포넌트는 다시 들어가지 않는다(순환 참조 대비). */
	@SuppressWarnings("rawtypes")
	private static void collectRefs(Schema root, Map<String, Schema> components, Set<String> names) {
		Deque<Schema> stack = new ArrayDeque<>();
		if (root != null) {
			stack.push(root);
		}
		while (!stack.isEmpty()) {
			Schema schema = stack.pop();
			String ref = schema.get$ref();
			if (ref != null) {
				if (ref.startsWith(SCHEMA_REF_PREFIX)) {
					String name = ref.substring(SCHEMA_REF_PREFIX.length());
					Schema target = components.get(name);
					if (names.add(name) && target != null) {
						stack.push(target);
					}
				}
				continue;
			}
			if (schema.getProperties() != null) {
				pushAll(stack, schema.getProperties().values());
			}
			if (schema.getItems() != null) {
				stack.push(schema.getItems());
			}
			if (schema.getAdditionalProperties() instanceof Schema additional) {
				stack.push(additional);
			}
			pushAll(stack, schema.getAllOf());
			pushAll(stack, schema.getAnyOf());
			pushAll(stack, schema.getOneOf());
			if (schema.getNot() != null) {
				stack.push(schema.getNot());
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private static void pushAll(Deque<Schema> stack, Collection<Schema> schemas) {
		if (schemas == null) {
			return;
		}
		for (Schema schema : schemas) {
			if (schema != null) {
				stack.push(schema);
			}
		}
	}

	/** OpenAPI 3.0의 nullable: true, 3.1의 type: [..., "null"], anyOf/oneOf 안의 null 타입을 모두 nullable로 본다. */
	@SuppressWarnings("rawtypes")
	private static boolean isNullable(Schema property) {
		if (Boolean.TRUE.equals(property.getNullable()) || isNullType(property)) {
			return true;
		}
		return containsNullType(property.getAnyOf()) || containsNullType(property.getOneOf());
	}

	@SuppressWarnings("rawtypes")
	private static boolean containsNullType(List<Schema> schemas) {
		if (schemas == null) {
			return false;
		}
		for (Schema schema : schemas) {
			if (schema != null && (isNullType(schema) || Boolean.TRUE.equals(schema.getNullable()))) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("rawtypes")
	private static boolean isNullType(Schema schema) {
		return (schema.getTypes() != null && schema.getTypes().contains(NULL_TYPE)) || NULL_TYPE.equals(schema.getType());
	}

	// ---- 공통 ----

	private static boolean isOpenApi31(OpenAPI openApi) {
		return SpecVersion.V31.equals(openApi.getSpecVersion())
				|| (openApi.getOpenapi() != null && openApi.getOpenapi().startsWith("3.1"));
	}

	/** OpenAPI 3.0(type)과 3.1(types) 어느 쪽으로 직렬화해도 string으로 나오는 스키마. */
	private static StringSchema stringSchema() {
		StringSchema schema = new StringSchema();
		schema.setTypes(new LinkedHashSet<>(List.of("string")));
		return schema;
	}
}
