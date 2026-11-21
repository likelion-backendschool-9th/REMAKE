package com.remake.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import com.remake.support.IntegrationTestSupport;

// OpenAPI 문서와 로컬 CORS 설정을 확인한다.
class WebConfigTest extends IntegrationTestSupport {

	@Test
	void OpenAPI_문서를_제공한다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Remake API"))
				.andExpect(jsonPath("$.components.securitySchemes.cookieAuth.type").value("apiKey"))
				.andExpect(jsonPath("$.components.securitySchemes.cookieAuth.in").value("cookie"))
				.andExpect(jsonPath("$.components.securitySchemes.cookieAuth.name").value("ACCESS_TOKEN"));
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
