package com.remake.recipe.adapter.out.ai;

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

	@Bean
	@Qualifier(GEMINI_REST_CLIENT)
	RestClient geminiRestClient(RestClient.Builder restClientBuilder, GeminiProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.requestTimeout());
		requestFactory.setReadTimeout(properties.requestTimeout());
		return restClientBuilder.clone()
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory)
				.build();
	}
}
