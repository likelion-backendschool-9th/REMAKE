package com.remake.ingredient;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.support.IntegrationTestSupport;

// 냉장고 목록·요약. 오늘은 시험용 시계의 2026-03-04이다.
class FridgeApiTest extends IntegrationTestSupport {

	private static final String URL = "/api/v1/user/ingredients";
	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	private Long me;
	private Long other;

	@BeforeEach
	void setUpUsers() {
		me = createUser();
		other = createUser();
	}

	private MockHttpServletRequestBuilder as(Long userId, MockHttpServletRequestBuilder builder) {
		return builder.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId);
	}

	private Long add(Long userId, String category, String name, int daysLeft) {
		return createIngredient(userId, category, name, TODAY.plusDays(daysLeft));
	}

	@Test
	void 로그인_정보가_없으면_401이다() throws Exception {
		mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
		mockMvc.perform(get(URL + "/summary")).andExpect(status().isUnauthorized());
	}

	@Test
	void 재료가_없으면_빈_목록과_0_요약을_돌려준다() throws Exception {
		mockMvc.perform(as(me, get(URL)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(0));
		mockMvc.perform(as(me, get(URL + "/summary")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(0))
				.andExpect(jsonPath("$.danger").value(0))
				.andExpect(jsonPath("$.soon").value(0))
				.andExpect(jsonPath("$.ok").value(0));
	}

	@Test
	void 소비기한이_빠른_순으로_dDay와_상태를_붙여_돌려준다() throws Exception {
		add(me, "VEGETABLE", "여유", 10);
		add(me, "MEAT", "지남", -2);
		add(me, "DAIRY_EGG", "당일", 0);
		add(me, "FRUIT", "내일", 1);
		add(me, "FRUIT", "사흘뒤", 3);
		add(me, "FRUIT", "나흘뒤", 4);

		mockMvc.perform(as(me, get(URL)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(6))
				.andExpect(jsonPath("$.items[0].name").value("지남"))
				.andExpect(jsonPath("$.items[0].dDay").value(-2))
				.andExpect(jsonPath("$.items[0].status").value("DANGER"))
				.andExpect(jsonPath("$.items[0].categoryCode").value("MEAT"))
				.andExpect(jsonPath("$.items[1].name").value("당일"))
				.andExpect(jsonPath("$.items[1].dDay").value(0))
				.andExpect(jsonPath("$.items[1].status").value("DANGER"))
				.andExpect(jsonPath("$.items[2].status").value("SOON"))
				.andExpect(jsonPath("$.items[3].dDay").value(3))
				.andExpect(jsonPath("$.items[3].status").value("SOON"))
				.andExpect(jsonPath("$.items[4].dDay").value(4))
				.andExpect(jsonPath("$.items[4].status").value("OK"))
				.andExpect(jsonPath("$.items[5].name").value("여유"))
				.andExpect(jsonPath("$.items[0].unit").value("EA"))
				.andExpect(jsonPath("$.items[0].imageUrl").isNotEmpty())
				// 목록에는 가격과 추정 여부를 주지 않는다(API 12). 가격은 상세(API 13)에만 있다.
				.andExpect(jsonPath("$.items[0].price").doesNotExist())
				.andExpect(jsonPath("$.items[0].isExpirationEstimated").doesNotExist());
	}

	@Test
	void 요약은_본인_재료만_세고_목록_상태와_같다() throws Exception {
		add(me, "MEAT", "지남", -1);
		add(me, "DAIRY_EGG", "당일", 0);
		add(me, "FRUIT", "내일", 1);
		add(me, "FRUIT", "사흘뒤", 3);
		add(me, "FRUIT", "나흘뒤", 4);
		add(other, "FRUIT", "남의것", 0);

		mockMvc.perform(as(me, get(URL + "/summary")))
				.andExpect(jsonPath("$.total").value(5))
				.andExpect(jsonPath("$.danger").value(2))
				.andExpect(jsonPath("$.soon").value(2))
				.andExpect(jsonPath("$.ok").value(1));
		mockMvc.perform(as(me, get(URL)))
				.andExpect(jsonPath("$.items.length()").value(5))
				.andExpect(jsonPath("$.items[?(@.status=='DANGER')]", hasSize(2)));
	}

	@Test
	void 다른_사용자의_재료는_목록에_나오지_않는다() throws Exception {
		add(me, "FRUIT", "사과", 5);
		add(other, "FRUIT", "남의것", 5);

		mockMvc.perform(as(me, get(URL)))
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].name").value("사과"));
	}

	@Test
	void 보관_위치와_카테고리로_거른다() throws Exception {
		Long frozen = add(me, "MEAT", "닭가슴살", 5);
		jdbcTemplate.update("UPDATE ingredients SET storage = 'FREEZER' WHERE id = ?", frozen);
		add(me, "MEAT", "돼지고기", 6);

		mockMvc.perform(as(me, get(URL).param("storage", "FREEZER")))
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].name").value("닭가슴살"));
		mockMvc.perform(as(me, get(URL).param("categoryCode", "MEAT")))
				.andExpect(jsonPath("$.items.length()").value(2));
		mockMvc.perform(as(me, get(URL).param("categoryCode", "MEAT").param("storage", "FREEZER")))
				.andExpect(jsonPath("$.items.length()").value(1));
	}

	@Test
	void 상태_카드로_거르고_다른_필터와_함께_쓸_수_있다() throws Exception {
		add(me, "MEAT", "지남", -2);
		add(me, "FRUIT", "당일", 0);
		add(me, "FRUIT", "사흘뒤", 3);
		add(me, "VEGETABLE", "여유", 10);

		mockMvc.perform(as(me, get(URL).param("status", "DANGER")))
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.items[0].name").value("지남"))
				.andExpect(jsonPath("$.items[1].name").value("당일"));
		mockMvc.perform(as(me, get(URL).param("status", "SOON")))
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].name").value("사흘뒤"));
		mockMvc.perform(as(me, get(URL).param("status", "OK")))
				.andExpect(jsonPath("$.items.length()").value(1));
		mockMvc.perform(as(me, get(URL).param("status", "DANGER").param("categoryCode", "FRUIT")))
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].name").value("당일"));
	}

	@Test
	void 잘못된_필터_값은_INVALID_INPUT이다() throws Exception {
		mockMvc.perform(as(me, get(URL).param("storage", "ATTIC")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		mockMvc.perform(as(me, get(URL).param("status", "EXPIRED")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		mockMvc.perform(as(me, get(URL).param("categoryCode", "NOPE")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void 날짜가_지나면_같은_재료의_상태가_바뀐다() throws Exception {
		add(me, "FRUIT", "사과", 4);
		mockMvc.perform(as(me, get(URL))).andExpect(jsonPath("$.items[0].status").value("OK"));

		clock.advance(Duration.ofDays(1));

		mockMvc.perform(as(me, get(URL)))
				.andExpect(jsonPath("$.items[0].dDay").value(3))
				.andExpect(jsonPath("$.items[0].status").value("SOON"));
		mockMvc.perform(as(me, get(URL + "/summary"))).andExpect(jsonPath("$.soon").value(1));
	}
}
