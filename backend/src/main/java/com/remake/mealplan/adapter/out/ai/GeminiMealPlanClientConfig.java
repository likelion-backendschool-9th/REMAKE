package com.remake.mealplan.adapter.out.ai;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.remake.global.config.GeminiProperties;

/**
 * 식단표 생성용 Gemini RestClient. 레시피 생성용(recipe GeminiClientConfig, 25초)과 따로 둔다.
 * 식단표는 한 번에 최대 15칸 레시피를 받아 답이 길고, 서버 작업이라 사용자가 화면에서 기다리지 않는다.
 */
@Configuration(proxyBeanMethods = false)
class GeminiMealPlanClientConfig {

	static final String GEMINI_MEAL_PLAN_REST_CLIENT = "geminiMealPlanRestClient";

	/**
	 * Gemini가 식단표 전체를 보내 줄 때까지 기다리는 시간. 어림(30~60초)보다 넉넉히 잡은 값이라 실측 뒤 고친다.
	 * 11-24 실측(냉장고 A, 15칸) 성공 6.5~14.3초. 재시도까지 합쳐도(과부하 재시도 쉬는 시간 74초 + 시간 초과 재시도 1회, 최악 약 5분) 끊긴 생성 정리 기준
	 * (MealPlanAbandonedJobService 10분) 안이다.
	 */
	static final Duration READ_TIMEOUT = Duration.ofSeconds(90);

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
}
