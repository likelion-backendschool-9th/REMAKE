package com.remake.recipe.adapter.out.ai;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.remake.global.config.GeminiProperties;

/**
 * Gemini 호출용 RestClient. 제한 시간을 여기서만 건다.
 * 어댑터는 완성된 RestClient를 받으므로, 어댑터 테스트는 MockRestServiceServer에 묶은 RestClient를 그대로 넘긴다.
 */
@Configuration(proxyBeanMethods = false)
class GeminiClientConfig {

	static final String GEMINI_REST_CLIENT = "geminiRestClient";

	/**
	 * 연결 제한. 구글 서버 연결은 보통 바로 되므로 짧게 둔다. 읽기 제한(requestTimeout, 25초)과 따로 걸려 있어서,
	 * 연결까지 25초로 두면 이론상 호출 한 번이 50초가 되어 #50의 "서버 55초 안" 규칙을 넘을 수 있다.
	 */
	static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

	@Bean
	@Qualifier(GEMINI_REST_CLIENT)
	RestClient geminiRestClient(RestClient.Builder restClientBuilder, GeminiProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
		requestFactory.setReadTimeout(properties.requestTimeout());
		return restClientBuilder.clone()
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory)
				.build();
	}
}
