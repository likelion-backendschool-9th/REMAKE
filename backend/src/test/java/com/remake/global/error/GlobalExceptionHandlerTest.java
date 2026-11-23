package com.remake.global.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import com.jayway.jsonpath.JsonPath;
import com.remake.global.common.TraceIdFilter;
import com.remake.support.IntegrationTestSupport;

// 모든 API가 같은 모양({ code, message, status, traceId, fieldErrors })으로 에러를 돌려주는지 확인한다.
class GlobalExceptionHandlerTest extends IntegrationTestSupport {

	@Test
	void BusinessException은_ErrorCode의_상태와_코드로_응답한다() throws Exception {
		mockMvc.perform(get("/test/errors/business"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CONCURRENT_UPDATE"))
				.andExpect(jsonPath("$.message").value(ErrorCode.CONCURRENT_UPDATE.getMessage()))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.traceId").isString())
				// fieldErrors는 값이 없어도 null로 주고, details는 값이 없으면 필드를 뺀다(컨벤션)
				.andExpect(jsonPath("$.fieldErrors").value(nullValue()))
				.andExpect(jsonPath("$.details").doesNotExist());
	}

	@Test
	void BusinessException의_details를_응답에_그대로_담는다() throws Exception {
		mockMvc.perform(get("/test/errors/business-details"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.message").value(ErrorCode.INVALID_INPUT.getMessage()))
				.andExpect(jsonPath("$.details.remaining").value(0))
				// 시각은 다른 응답과 같이 +09:00을 붙인다(JacksonConfig)
				.andExpect(jsonPath("$.details.resetsAt").value("2026-11-21T00:00:00+09:00"))
				.andExpect(jsonPath("$.details.note").value(nullValue()))
				.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
	}

	@Test
	void BusinessException의_메시지를_바꿔_보낼_수_있다() throws Exception {
		mockMvc.perform(get("/test/errors/business-message"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.message").value("남은 수량은 300g입니다."));
	}

	@Test
	void 요청_본문_검증_실패는_fieldErrors로_알려준다() throws Exception {
		mockMvc.perform(post("/test/errors/body").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"\", \"amount\": 0}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.fieldErrors.length()").value(2))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'amount')]").exists());
	}

	@Test
	void 경로_변수_검증_실패도_fieldErrors로_알려준다() throws Exception {
		mockMvc.perform(get("/test/errors/path/0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("amount"));
	}

	@Test
	void JSON_형식이_틀리면_400() throws Exception {
		mockMvc.perform(post("/test/errors/body").contentType(MediaType.APPLICATION_JSON).content("{이상한 JSON"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void 타입이_맞지_않는_경로_변수는_400() throws Exception {
		mockMvc.perform(get("/test/errors/path/abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void 지원하지_않는_요청_형식은_415() throws Exception {
		mockMvc.perform(post("/test/errors/body").contentType(MediaType.TEXT_PLAIN).content("hello"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
	}

	@Test
	void 없는_주소는_404() throws Exception {
		// /api/v1/user/** 는 로그인이 필요하므로, 인증된 상태에서 핸들러 없음(404)을 확인한다.
		mockMvc.perform(get("/api/v1/user/no-such-path")
						.requestAttr(com.remake.global.web.LoginUserIdArgumentResolver.ATTRIBUTE, 1L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void 허용하지_않는_메서드는_405() throws Exception {
		mockMvc.perform(delete("/test/errors/business"))
				.andExpect(status().isMethodNotAllowed())
				// 허용하는 메서드를 알려 주는 Allow 헤더를 빼지 않는다
				.andExpect(header().string(HttpHeaders.ALLOW, "GET"))
				.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
	}

	@Test
	void 필수_헤더가_없으면_400() throws Exception {
		mockMvc.perform(get("/test/errors/header"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void 필수_파일이_없으면_400() throws Exception {
		mockMvc.perform(multipart("/test/errors/part").param("other", "value"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void 필수_쿠키가_없으면_400() throws Exception {
		mockMvc.perform(get("/test/errors/cookie"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void ResponseStatusException은_원래_상태로_응답하고_사유를_숨긴다() throws Exception {
		mockMvc.perform(get("/test/errors/status"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"))
				.andExpect(jsonPath("$.message").value(ErrorCode.NOT_FOUND.getMessage()))
				.andExpect(jsonPath("$.traceId").isString());
	}

	@Test
	void 예상하지_못한_오류는_500이고_내부_내용을_숨긴다() throws Exception {
		mockMvc.perform(get("/test/errors/unexpected"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_ERROR.getMessage()));
	}

	@Test
	void 에러_응답의_traceId는_응답_헤더의_추적_ID와_같다() throws Exception {
		MockHttpServletResponse response = mockMvc.perform(get("/test/errors/business"))
				.andExpect(header().string(TraceIdFilter.HEADER, matchesPattern("[0-9a-f]{16}")))
				.andReturn().getResponse();

		String bodyTraceId = JsonPath.read(response.getContentAsString(), "$.traceId");
		assertThat(bodyTraceId).isEqualTo(response.getHeader(TraceIdFilter.HEADER));
	}
}
