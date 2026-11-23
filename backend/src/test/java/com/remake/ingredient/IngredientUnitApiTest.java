package com.remake.ingredient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.support.IntegrationTestSupport;

// API 14: 이름(필수)과 카테고리(선택)로 단위를 정해 준다. 카테고리가 있으면 저장할 때와 같은 단위이고,
// 없으면 사전 이름과 정확히 같을 때만 사전 단위이고 아니면 EA이다.
class IngredientUnitApiTest extends IntegrationTestSupport {

	private static final String URL = "/api/v1/user/ingredient-units";

	private MockHttpServletRequestBuilder login(MockHttpServletRequestBuilder request) {
		return request.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, 1L);
	}

	@Test
	void 로그인_정보가_없으면_401이다() throws Exception {
		mockMvc.perform(get(URL).param("name", "대파")).andExpect(status().isUnauthorized());
	}

	@Test
	void 사전_이름과_같으면_사전_단위를_준다() throws Exception {
		mockMvc.perform(login(get(URL).param("name", "대파")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.unit").value("BUNCH"));
		mockMvc.perform(login(get(URL).param("name", " 우유 ")))
				.andExpect(jsonPath("$.unit").value("PACK"));
	}

	@Test
	void 사전에_없는_이름은_EA다() throws Exception {
		mockMvc.perform(login(get(URL).param("name", "없는재료이름")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.unit").value("EA"));
	}

	@Test
	void 카테고리가_없으면_수식어가_붙은_이름은_EA다() throws Exception {
		mockMvc.perform(login(get(URL).param("name", "유기농 우유")))
				.andExpect(jsonPath("$.unit").value("EA"));
	}

	@Test
	void 카테고리를_넣으면_저장할_때와_같은_단위를_준다() throws Exception {
		// 같은 카테고리의 사전 이름이 들어 있으면 그 단위다
		mockMvc.perform(login(get(URL).param("name", "유기농 우유").param("categoryCode", "DAIRY_EGG")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.unit").value("PACK"));
		// 카테고리가 다르면 들어 있어도 쓰지 않는다
		mockMvc.perform(login(get(URL).param("name", "유기농 우유").param("categoryCode", "VEGETABLE")))
				.andExpect(jsonPath("$.unit").value("EA"));
		// 사전 이름과 정확히 같으면 카테고리와 상관없이 사전 단위다
		mockMvc.perform(login(get(URL).param("name", "대파").param("categoryCode", "MEAT")))
				.andExpect(jsonPath("$.unit").value("BUNCH"));
	}

	@Test
	void 없는_카테고리_코드는_INVALID_INPUT이다() throws Exception {
		mockMvc.perform(login(get(URL).param("name", "우유").param("categoryCode", "NOPE")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void 이름이_없거나_비어_있으면_INVALID_INPUT이다() throws Exception {
		mockMvc.perform(login(get(URL)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		mockMvc.perform(login(get(URL).param("name", "   ")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}
}
