package com.remake.mealplan.adapter.out.ai;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.remake.global.config.GeminiProperties;

/**
 * 식단표 생성용 Gemini RestClient. 레시피 생성용(recipe GeminiClientConfig, 연결 5초·읽기 20초)과 따로 둔다.
 * 식단표는 한 번에 최대 15칸 레시피를 받아 답이 길고, 서버 작업이라 사용자가 화면에서 기다리지 않는다.
 */
@Configuration(proxyBeanMethods = false)
class GeminiMealPlanClientConfig {

	static final String GEMINI_MEAL_PLAN_REST_CLIENT = "geminiMealPlanRestClient";
	static final String GEMINI_MEAL_SLOT_REPLACE_REST_CLIENT = "geminiMealSlotReplaceRestClient";

	/**
	 * Gemini가 식단표 전체를 보내 줄 때까지 기다리는 시간. 어림(30~60초)보다 넉넉히 잡은 값이라 실측 뒤 고친다.
	 * 11-24 실측(냉장고 A, 15칸) 성공 6.5~14.3초. 재시도까지 합쳐도(과부하 재시도 쉬는 시간 74초 + 시간 초과 재시도 1회, 최악 약 5분) 끊긴 생성 정리 기준
	 * (MealPlanAbandonedJobService 10분) 안이다.
	 */
	static final Duration READ_TIMEOUT = Duration.ofSeconds(90);

	/** 식단 칸 교체 연결 제한. 레시피 생성(recipe GeminiClientConfig)과 같다. 구글 서버 연결은 보통 바로 되므로 짧게 둔다. */
	static final Duration SLOT_REPLACE_CONNECT_TIMEOUT = Duration.ofSeconds(5);

	/**
	 * 식단 칸 교체 읽기 제한. 연결과 따로 걸리므로 호출 한 번은 최대 5 + 20 = 25초이고,
	 * MealSlotReplaceService가 1회 재시도하고 1.5초 쉬므로 가장 나쁜 경우 25 × 2 + 1.5 = 51.5초로 #50의 "서버 55초 안"(Nginx 60초보다 먼저)을 지킨다.
	 */
	static final Duration SLOT_REPLACE_READ_TIMEOUT = Duration.ofSeconds(20);

	@Bean
	@Qualifier(GEMINI_MEAL_PLAN_REST_CLIENT)
	RestClient geminiMealPlanRestClient(RestClient.Builder restClientBuilder, GeminiProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.requestTimeout());
		requestFactory.setReadTimeout(READ_TIMEOUT);
		return restClientBuilder.clone()
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory)
				.build();
	}

	/**
	 * 식단 칸 교체(API 42)용. 사용자가 응답을 기다리므로 연결 5초·읽기 20초(호출 한 번 최대 25초)를 쓴다.
	 * 칸 하나라 답이 짧다. 1회 재시도까지 합쳐 Nginx 60초 안에 끝난다(#50).
	 */
	@Bean
	@Qualifier(GEMINI_MEAL_SLOT_REPLACE_REST_CLIENT)
	RestClient geminiMealSlotReplaceRestClient(RestClient.Builder restClientBuilder, GeminiProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(SLOT_REPLACE_CONNECT_TIMEOUT);
		requestFactory.setReadTimeout(SLOT_REPLACE_READ_TIMEOUT);
		return restClientBuilder.clone()
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory)
				.build();
	}
}
