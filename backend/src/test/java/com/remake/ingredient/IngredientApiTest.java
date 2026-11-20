package com.remake.ingredient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.ingredient.domain.IngredientStatus;
import com.remake.support.IntegrationTestSupport;

// 재료 API. 통합 테스트는 요청 속성으로 로그인 사용자를 넣는다(AuthCookieFilter가 SecurityContext로 옮긴다).
class IngredientApiTest extends IntegrationTestSupport {

	private static final String URL = "/api/v1/user/ingredients";
	private static final Long ME = 101L;
	private static final Long OTHER = 102L;
	private static final LocalDate EXPIRATION = LocalDate.of(2026, 3, 12);

	@BeforeEach
	void setUpUsers() {
		ensureUser(ME);
		ensureUser(OTHER);
	}

	private MockHttpServletRequestBuilder as(Long userId, MockHttpServletRequestBuilder builder) {
		return builder.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId);
	}

	private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
		return builder.contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private static String createBody(String unit) {
		return """
				{ "name": "우유", "categoryCode": "DAIRY_EGG", "storage": "FRIDGE", "expirationDate": "2026-03-12",
				  "amount": 2, "unit": "%s", "price": 3000 }
				""".formatted(unit);
	}

	private Long createMilk(Long userId) {
		return createIngredient(userId, "DAIRY_EGG", "우유", EXPIRATION, IngredientStatus.ACTIVE, 3000L);
	}

	@Test
	void 로그인_정보가_없으면_401이다() throws Exception {
		mockMvc.perform(get(URL + "/1"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void 재료를_추가하고_상세로_다시_조회한다() throws Exception {
		mockMvc.perform(json(as(ME, post(URL)), createBody("PACK")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("우유"))
				.andExpect(jsonPath("$.categoryCode").value("DAIRY_EGG"))
				.andExpect(jsonPath("$.storage").value("FRIDGE"))
				.andExpect(jsonPath("$.expirationDate").value("2026-03-12"))
				.andExpect(jsonPath("$.isExpirationEstimated").value(false))
				.andExpect(jsonPath("$.unit").value("PACK"))
				.andExpect(jsonPath("$.price").value(3000))
				.andExpect(jsonPath("$.status").value("ACTIVE"));

		Long id = jdbcTemplate.queryForObject("SELECT id FROM ingredients", Long.class);
		mockMvc.perform(as(ME, get(URL + "/" + id)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.amount").value(2));
	}

	@Test
	void 냉장고_단위가_아니면_INVALID_UNIT이다() throws Exception {
		mockMvc.perform(json(as(ME, post(URL)), createBody("TBSP")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_UNIT"));
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredients", Long.class)).isZero();
	}

	@Test
	void 잘못된_입력은_INVALID_INPUT이고_틀린_칸을_알려_준다() throws Exception {
		String body = """
				{ "name": "", "categoryCode": "DAIRY_EGG", "storage": "FRIDGE", "expirationDate": "2026-03-12",
				  "amount": 0, "unit": "EA", "price": -1 }
				""";

		mockMvc.perform(json(as(ME, post(URL)), body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'amount')]").exists())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'price')]").exists());
	}

	@Test
	void 필수값이_빠지면_INVALID_INPUT이다() throws Exception {
		mockMvc.perform(json(as(ME, post(URL)), "{ \"name\": \"우유\" }"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void 다른_사용자의_재료는_조회_수정_삭제_모두_404이다() throws Exception {
		Long id = createMilk(OTHER);

		mockMvc.perform(as(ME, get(URL + "/" + id)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("INGREDIENT_NOT_FOUND"));
		mockMvc.perform(json(as(ME, patch(URL + "/" + id)), "{ \"storage\": \"FREEZER\" }"))
				.andExpect(status().isNotFound());
		mockMvc.perform(as(ME, delete(URL + "/" + id)))
				.andExpect(status().isNotFound());
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredients", Long.class)).isEqualTo(1);
	}

	@Test
	void 수정하면_보낸_값만_바뀌고_소비기한을_고치면_추정_표시가_꺼진다() throws Exception {
		Long id = createMilk(ME);
		jdbcTemplate.update("UPDATE ingredients SET is_expiration_estimated = true WHERE id = ?", id);

		String body = """
				{ "name": "  두유  ", "categoryCode": "BEAN_TOFU", "expirationDate": "2026-03-20" }
				""";
		mockMvc.perform(json(as(ME, patch(URL + "/" + id)), body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("두유"))
				.andExpect(jsonPath("$.categoryCode").value("BEAN_TOFU"))
				.andExpect(jsonPath("$.expirationDate").value("2026-03-20"))
				.andExpect(jsonPath("$.isExpirationEstimated").value(false))
				.andExpect(jsonPath("$.storage").value("FRIDGE"))
				.andExpect(jsonPath("$.price").value(3000));
	}

	@Test
	void 가격은_키를_보내지_않으면_그대로이고_null이면_지운다() throws Exception {
		Long id = createMilk(ME);

		mockMvc.perform(json(as(ME, patch(URL + "/" + id)), "{ \"storage\": \"FREEZER\" }"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.storage").value("FREEZER"))
				.andExpect(jsonPath("$.price").value(3000));

		mockMvc.perform(json(as(ME, patch(URL + "/" + id)), "{ \"price\": 2500 }"))
				.andExpect(jsonPath("$.price").value(2500));

		mockMvc.perform(json(as(ME, patch(URL + "/" + id)), "{ \"price\": null }"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.price").doesNotExist());
	}

	@Test
	void 수정의_잘못된_입력은_INVALID_INPUT이고_값이_바뀌지_않는다() throws Exception {
		Long id = createMilk(ME);

		mockMvc.perform(json(as(ME, patch(URL + "/" + id)), "{ \"name\": \"   \", \"price\": -1 }"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'price')]").exists());

		assertThat(jdbcTemplate.queryForObject("SELECT name FROM ingredients WHERE id = ?", String.class, id))
				.isEqualTo("우유");
	}

	@Test
	void 삭제하면_204이고_다시_조회하면_404이다() throws Exception {
		Long id = createMilk(ME);

		mockMvc.perform(as(ME, delete(URL + "/" + id))).andExpect(status().isNoContent());
		mockMvc.perform(as(ME, get(URL + "/" + id))).andExpect(status().isNotFound());
	}

	@Test
	void 수량을_고치고_사용한_재료는_거부하며_단위는_바꿀_수_없다() throws Exception {
		Long id = createMilk(ME);

		mockMvc.perform(json(as(ME, patch(URL + "/" + id)), "{ \"amount\": 4, \"unit\": \"KG\" }"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.amount").value(4))
				.andExpect(jsonPath("$.unit").value("EA"));

		jdbcTemplate.update("UPDATE ingredients SET amount = 1 WHERE id = ?", id);
		mockMvc.perform(json(as(ME, patch(URL + "/" + id)), "{ \"amount\": 9 }"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));

		mockMvc.perform(json(as(ME, patch(URL + "/" + id)), "{ \"amount\": 0 }"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'amount')]").exists());
	}
}
