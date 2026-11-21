package com.remake.ingredient;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.ingredient.domain.IngredientStatus;
import com.remake.support.IntegrationTestSupport;

// 냉장고 목록·요약. 오늘은 시험용 시계의 2026-03-04이다.
class FridgeApiTest extends IntegrationTestSupport {

	private static final String URL = "/api/v1/user/ingredients";
	private static final Long ME = 101L;
	private static final Long OTHER = 102L;
	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	@BeforeEach
	void setUpUsers() {
		ensureUser(ME);
		ensureUser(OTHER);
	}

	private MockHttpServletRequestBuilder as(Long userId, MockHttpServletRequestBuilder builder) {
		return builder.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId);
	}

	private Long add(Long userId, String category, String name, int daysLeft) {
		return createIngredient(userId, category, name, TODAY.plusDays(daysLeft), IngredientStatus.ACTIVE);
	}

	@Test
	void 로그인_정보가_없으면_401이다() throws Exception {
		mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
		mockMvc.perform(get(URL + "/summary")).andExpect(status().isUnauthorized());
	}

	@Test
	void 재료가_없으면_빈_목록과_0_요약을_돌려준다() throws Exception {
		mockMvc.perform(as(ME, get(URL)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(as(ME, get(URL + "/summary")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(0))
				.andExpect(jsonPath("$.danger").value(0))
				.andExpect(jsonPath("$.soon").value(0))
				.andExpect(jsonPath("$.ok").value(0));
	}

	@Test
	void 소비기한이_빠른_순으로_dDay와_상태를_붙여_돌려준다() throws Exception {
		add(ME, "VEGETABLE", "여유", 10);
		add(ME, "MEAT", "지남", -2);
		add(ME, "DAIRY_EGG", "당일", 0);
		add(ME, "FRUIT", "내일", 1);
		add(ME, "FRUIT", "사흘뒤", 3);
		add(ME, "FRUIT", "나흘뒤", 4);

		mockMvc.perform(as(ME, get(URL)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(6))
				.andExpect(jsonPath("$[0].name").value("지남"))
				.andExpect(jsonPath("$[0].dDay").value(-2))
				.andExpect(jsonPath("$[0].status").value("DANGER"))
				.andExpect(jsonPath("$[0].categoryCode").value("MEAT"))
				.andExpect(jsonPath("$[1].name").value("당일"))
				.andExpect(jsonPath("$[1].dDay").value(0))
				.andExpect(jsonPath("$[1].status").value("DANGER"))
				.andExpect(jsonPath("$[2].status").value("SOON"))
				.andExpect(jsonPath("$[3].dDay").value(3))
				.andExpect(jsonPath("$[3].status").value("SOON"))
				.andExpect(jsonPath("$[4].dDay").value(4))
				.andExpect(jsonPath("$[4].status").value("OK"))
				.andExpect(jsonPath("$[5].name").value("여유"))
				.andExpect(jsonPath("$[0].isExpirationEstimated").value(false))
				.andExpect(jsonPath("$[0].unit").value("EA"));
	}

	@Test
	void 요약은_진행_중인_본인_재료만_세고_목록_상태와_같다() throws Exception {
		add(ME, "MEAT", "지남", -1);
		add(ME, "DAIRY_EGG", "당일", 0);
		add(ME, "FRUIT", "내일", 1);
		add(ME, "FRUIT", "사흘뒤", 3);
		add(ME, "FRUIT", "나흘뒤", 4);
		createIngredient(ME, "FRUIT", "완료", TODAY.minusDays(5), IngredientStatus.FINISHED);
		add(OTHER, "FRUIT", "남의것", 0);

		mockMvc.perform(as(ME, get(URL + "/summary")))
				.andExpect(jsonPath("$.total").value(5))
				.andExpect(jsonPath("$.danger").value(2))
				.andExpect(jsonPath("$.soon").value(2))
				.andExpect(jsonPath("$.ok").value(1));
		mockMvc.perform(as(ME, get(URL)))
				.andExpect(jsonPath("$.length()").value(5))
				.andExpect(jsonPath("$[?(@.status=='DANGER')]", hasSize(2)));
	}

	@Test
	void 완료된_재료와_다른_사용자의_재료는_목록에_나오지_않는다() throws Exception {
		add(ME, "FRUIT", "사과", 5);
		createIngredient(ME, "FRUIT", "완료", TODAY.plusDays(1), IngredientStatus.FINISHED);
		add(OTHER, "FRUIT", "남의것", 5);

		mockMvc.perform(as(ME, get(URL)))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].name").value("사과"));
	}

	@Test
	void 보관_위치와_카테고리와_검색어로_거른다() throws Exception {
		Long frozen = add(ME, "MEAT", "닭가슴살", 5);
		jdbcTemplate.update("UPDATE ingredients SET storage = 'FREEZER' WHERE id = ?", frozen);
		add(ME, "MEAT", "돼지고기", 6);
		add(ME, "VEGETABLE", "Apple 양파", 7);
		add(ME, "VEGETABLE", "100% 주스", 8);

		mockMvc.perform(as(ME, get(URL).param("storageType", "FREEZER")))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].name").value("닭가슴살"));
		mockMvc.perform(as(ME, get(URL).param("categoryCode", "MEAT")))
				.andExpect(jsonPath("$.length()").value(2));
		mockMvc.perform(as(ME, get(URL).param("q", "apple")))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].name").value("Apple 양파"));
		mockMvc.perform(as(ME, get(URL).param("q", "%")))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].name").value("100% 주스"));
		mockMvc.perform(as(ME, get(URL).param("categoryCode", "MEAT").param("storageType", "FREEZER").param("q", "닭")))
				.andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(as(ME, get(URL).param("q", "   ")))
				.andExpect(jsonPath("$.length()").value(4));
	}

	@Test
	void 잘못된_필터_값은_INVALID_INPUT이다() throws Exception {
		mockMvc.perform(as(ME, get(URL).param("storageType", "ATTIC")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		mockMvc.perform(as(ME, get(URL).param("categoryCode", "NOPE")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		mockMvc.perform(as(ME, get(URL).param("q", " " + "가".repeat(50) + " ")))
				.andExpect(status().isOk());
		mockMvc.perform(as(ME, get(URL).param("q", "가".repeat(51))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void 날짜가_지나면_같은_재료의_상태가_바뀐다() throws Exception {
		add(ME, "FRUIT", "사과", 4);
		mockMvc.perform(as(ME, get(URL))).andExpect(jsonPath("$[0].status").value("OK"));

		clock.advance(java.time.Duration.ofDays(1));

		mockMvc.perform(as(ME, get(URL)))
				.andExpect(jsonPath("$[0].dDay").value(3))
				.andExpect(jsonPath("$[0].status").value("SOON"));
		mockMvc.perform(as(ME, get(URL + "/summary"))).andExpect(jsonPath("$.soon").value(1));
	}
}
