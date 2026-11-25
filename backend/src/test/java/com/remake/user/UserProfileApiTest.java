package com.remake.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.support.IntegrationTestSupport;

class UserProfileApiTest extends IntegrationTestSupport {

	private Long me;

	@BeforeEach
	void setUpUser() {
		me = createUser();
	}

	private MockHttpServletRequestBuilder asMe(MockHttpServletRequestBuilder builder) {
		return builder.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, me);
	}

	@Test
	void 온보딩_저장과_프로필_조회_그리고_이벤트를_남긴다() throws Exception {
		mockMvc.perform(asMe(get("/api/v1/user/profile")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.provider").value("KAKAO"))
				.andExpect(jsonPath("$.plan").value("BASIC"))
				.andExpect(jsonPath("$.householdSize").doesNotExist())
				.andExpect(jsonPath("$.excludedIngredientCount").value(0))
				.andExpect(jsonPath("$.recipeSupplementEnabled").value(false))
				.andExpect(jsonPath("$.notificationEnabled").value(true))
				.andExpect(jsonPath("$.onboardingCompleted").value(false));

		mockMvc.perform(asMe(put("/api/v1/user/onboarding"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "householdSize": 2, "allergenCodes": ["EGG", "MILK"] }
								"""))
				.andExpect(status().isNoContent());

		mockMvc.perform(asMe(get("/api/v1/user/profile")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.householdSize").value(2))
				.andExpect(jsonPath("$.excludedIngredientCount").value(2))
				.andExpect(jsonPath("$.onboardingCompleted").value(true));

		mockMvc.perform(asMe(get("/api/v1/user/excluded-ingredients")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.allergenCodes.length()").value(2))
				.andExpect(jsonPath("$.allergenCodes[0]").value("EGG"))
				.andExpect(jsonPath("$.allergenCodes[1]").value("MILK"));

		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'onboarding_completed'", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT properties->>'excluded_count' FROM events WHERE event_name = 'onboarding_completed'",
				String.class)).isEqualTo("2");
	}

	@Test
	void 온보딩_알레르기_0개도_저장된다() throws Exception {
		mockMvc.perform(asMe(put("/api/v1/user/onboarding"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "householdSize": 1, "allergenCodes": [] }
								"""))
				.andExpect(status().isNoContent());

		mockMvc.perform(asMe(get("/api/v1/user/profile")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.excludedIngredientCount").value(0))
				.andExpect(jsonPath("$.onboardingCompleted").value(true));

		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT properties->>'excluded_count' FROM events WHERE event_name = 'onboarding_completed'",
				String.class)).isEqualTo("0");
	}

	@Test
	void 온보딩을_두_번_하면_INVALID_INPUT이다() throws Exception {
		mockMvc.perform(asMe(put("/api/v1/user/onboarding"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "householdSize": 1, "allergenCodes": [] }
								"""))
				.andExpect(status().isNoContent());

		mockMvc.perform(asMe(put("/api/v1/user/onboarding"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "householdSize": 3, "allergenCodes": ["PEANUT"] }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void 가구원_수_범위와_모르는_알레르기_코드는_INVALID_INPUT이다() throws Exception {
		mockMvc.perform(asMe(put("/api/v1/user/onboarding"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "householdSize": 0, "allergenCodes": [] }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));

		mockMvc.perform(asMe(put("/api/v1/user/onboarding"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "householdSize": 2, "allergenCodes": ["UNKNOWN_FOOD"] }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void 프로필_부분_수정과_제외_재료_교체() throws Exception {
		mockMvc.perform(asMe(put("/api/v1/user/onboarding"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "householdSize": 2, "allergenCodes": ["EGG"] }
								"""))
				.andExpect(status().isNoContent());

		mockMvc.perform(asMe(patch("/api/v1/user/profile"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "recipeSupplementEnabled": true }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.householdSize").value(2))
				.andExpect(jsonPath("$.recipeSupplementEnabled").value(true))
				.andExpect(jsonPath("$.notificationEnabled").value(true));

		mockMvc.perform(asMe(put("/api/v1/user/excluded-ingredients"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "allergenCodes": ["PEANUT", "WHEAT"] }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.allergenCodes.length()").value(2));

		mockMvc.perform(asMe(put("/api/v1/user/excluded-ingredients"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "allergenCodes": [] }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.allergenCodes.length()").value(0));

		mockMvc.perform(asMe(get("/api/v1/user/profile")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.excludedIngredientCount").value(0));
	}
}
