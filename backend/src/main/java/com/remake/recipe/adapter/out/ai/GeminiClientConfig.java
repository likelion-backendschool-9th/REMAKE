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

	/** 연결 제한. 구글 서버 연결은 보통 바로 되므로 짧게 둔다. */
	static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

	/**
	 * 읽기 제한(요청을 보낸 뒤 응답을 기다리는 시간). 연결과 따로 걸리므로 호출 한 번은 최대 5 + 20 = 25초이고,
	 * Service가 1회 재시도하고 1.5초 쉬므로 가장 나쁜 경우 25 × 2 + 1.5 = 51.5초로 #50의 "서버 55초 안"(Nginx 60초보다 먼저)을 지킨다.
	 */
	static final Duration READ_TIMEOUT = Duration.ofSeconds(20);

	@Bean
	@Qualifier(GEMINI_REST_CLIENT)
	RestClient geminiRestClient(RestClient.Builder restClientBuilder, GeminiProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
		requestFactory.setReadTimeout(READ_TIMEOUT);
		return restClientBuilder.clone()
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory)
				.build();
	}
}
