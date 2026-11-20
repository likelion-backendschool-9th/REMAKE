package com.remake.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.remake.support.IntegrationTestSupport;

// JSON 시각 표기: LocalDateTime은 +09:00을 붙여 내보내고, 받을 때는 한국 시간으로 바꾼다.
class JacksonConfigTest extends IntegrationTestSupport {

	@Test
	void 시각은_한국_시간대를_붙이고_초_아래는_버린다() throws Exception {
		mockMvc.perform(get("/test/time"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.at").value("2026-11-15T09:00:00+09:00"))
				// 날짜만 있는 값은 그대로
				.andExpect(jsonPath("$.date").value("2026-12-21"));
	}

	@Test
	void 다른_시간대로_받은_시각은_한국_시간으로_바꾼다() throws Exception {
		mockMvc.perform(post("/test/time").contentType(MediaType.APPLICATION_JSON)
						.content("{\"at\": \"2026-11-15T00:00:00Z\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.at").value("2026-11-15T09:00:00+09:00"));
	}

	@Test
	void 시간대_없이_받은_시각은_한국_시간으로_본다() throws Exception {
		mockMvc.perform(post("/test/time").contentType(MediaType.APPLICATION_JSON)
						.content("{\"at\": \"2026-11-15T09:00:00\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.at").value("2026-11-15T09:00:00+09:00"));
	}

	@Test
	void 시각_형식이_틀리면_400() throws Exception {
		mockMvc.perform(post("/test/time").contentType(MediaType.APPLICATION_JSON)
						.content("{\"at\": \"어제 아침\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}
}
