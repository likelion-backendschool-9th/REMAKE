package com.remake.global.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Gemini(AI 레시피·식단표 생성) 설정. 키는 로컬은 backend/.env, 운영은 서버 .env의 GEMINI_API_KEY(compose.prod.yml이 넘김)에 넣는다.
 * 키가 비어 있으면 호출하지 않고, 생성 요청은 AI_UPSTREAM_ERROR로 실패한다.
 *
 * @param apiKey         Google AI Studio API 키. 비어 있으면 호출하지 않는다
 * @param model          모델 코드. 기본 gemini-3.5-flash-lite(무료 티어)
 * @param baseUrl        API 주소. 테스트는 접속되지 않는 주소를 넣는다
 * @param requestTimeout 식단 칸 교체의 호출 한 번 제한 시간. 기본 25초. 레시피 생성은 recipe GeminiClientConfig의 값(연결 5초, 읽기 20초)을 쓴다
 */
@ConfigurationProperties(prefix = "remake.gemini")
public record GeminiProperties(String apiKey, String model, String baseUrl, Duration requestTimeout) {

	public static final String DEFAULT_MODEL = "gemini-3.5-flash-lite";
	public static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com";
	public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(25);

	public GeminiProperties {
		model = StringUtils.hasText(model) ? model : DEFAULT_MODEL;
		baseUrl = StringUtils.hasText(baseUrl) ? baseUrl : DEFAULT_BASE_URL;
		requestTimeout = requestTimeout != null ? requestTimeout : DEFAULT_REQUEST_TIMEOUT;
	}

	public boolean hasApiKey() {
		return StringUtils.hasText(apiKey);
	}
}
