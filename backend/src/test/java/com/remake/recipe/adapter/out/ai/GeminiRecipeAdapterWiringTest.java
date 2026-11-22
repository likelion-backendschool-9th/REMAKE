package com.remake.recipe.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.RestClient;

import com.remake.global.config.GeminiProperties;
import com.remake.support.IntegrationTestSupport;

import tools.jackson.databind.json.JsonMapper;

// 통합 테스트는 어댑터를 가짜로 바꾸므로 실제 생성자는 돌지 않는다. 생성자 인자가 서버에서도 주입되는지 여기서 확인한다.
class GeminiRecipeAdapterWiringTest extends IntegrationTestSupport {

	@Autowired
	private ApplicationContext context;

	@Test
	void 실제_어댑터의_생성자_인자가_모두_빈으로_있다() {
		RestClient restClient = context.getBean(GeminiClientConfig.GEMINI_REST_CLIENT, RestClient.class);
		GeminiProperties properties = context.getBean(GeminiProperties.class);
		JsonMapper jsonMapper = context.getBean(JsonMapper.class);

		assertThat(new GeminiRecipeAdapter(restClient, properties, jsonMapper)).isNotNull();
		assertThat(properties.model()).isEqualTo("test-model");
		assertThat(properties.baseUrl()).isEqualTo("http://localhost:1");
	}
}
