package com.remake.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.support.IntegrationTestSupport;

class NotificationApiTest extends IntegrationTestSupport {

	private static final String BASE = "/api/v1/user/notifications";
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

	private void insertNotification(Long userId, String type, String message, LocalDateTime createdAt,
			LocalDateTime readAt) {
		jdbcTemplate.update("""
				INSERT INTO notifications (user_id, type, message, read_at, created_at)
				VALUES (?, ?, ?, ?, ?)
				""", userId, type, message, readAt, createdAt);
	}

	@Test
	void 로그인_없으면_401이다() throws Exception {
		mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
		mockMvc.perform(get(BASE + "/summary")).andExpect(status().isUnauthorized());
	}

	@Test
	void 알림이_없으면_요약은_빈_값이다() throws Exception {
		mockMvc.perform(as(me, get(BASE + "/summary")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.latestMessage").doesNotExist())
				.andExpect(jsonPath("$.unreadCount").value(0));
	}

	@Test
	void 요약은_최근_문구와_안_읽은_수를_돌려준다() throws Exception {
		LocalDateTime t0 = LocalDateTime.now(clock);
		insertNotification(me, "EXPIRED", "오래된", t0, t0);
		clock.advance(Duration.ofMinutes(1));
		LocalDateTime t1 = LocalDateTime.now(clock);
		insertNotification(me, "EXPIRY_SOON", "최신 임박", t1, null);
		insertNotification(other, "EXPIRED", "남의것", t1, null);

		mockMvc.perform(as(me, get(BASE + "/summary")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.latestMessage").value("최신 임박"))
				.andExpect(jsonPath("$.unreadCount").value(1));
	}

	@Test
	void 목록은_최근순이고_조회하면_모두_읽음이_된다() throws Exception {
		LocalDateTime t0 = LocalDateTime.now(clock);
		insertNotification(me, "EXPIRED", "첫번째", t0, null);
		clock.advance(Duration.ofMinutes(1));
		LocalDateTime t1 = LocalDateTime.now(clock);
		insertNotification(me, "EXPIRY_SOON", "두번째", t1, null);

		mockMvc.perform(as(me, get(BASE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(2))
				.andExpect(jsonPath("$.content[0].message").value("두번째"))
				.andExpect(jsonPath("$.content[0].type").value("EXPIRY_SOON"))
				.andExpect(jsonPath("$.content[0].mealPlanId").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.content[1].message").value("첫번째"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.hasNext").value(false));

		Integer unread = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM notifications WHERE user_id = ? AND read_at IS NULL",
				Integer.class, me);
		assertThat(unread).isZero();

		mockMvc.perform(as(me, get(BASE + "/summary")))
				.andExpect(jsonPath("$.unreadCount").value(0))
				.andExpect(jsonPath("$.latestMessage").value("두번째"));
	}

	@Test
	void 남의_알림은_목록에_나오지_않는다() throws Exception {
		insertNotification(other, "EXPIRED", "남의것", LocalDateTime.now(clock), null);

		mockMvc.perform(as(me, get(BASE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(0))
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void 목록은_페이징하고_다음_페이지가_있으면_hasNext다() throws Exception {
		// EXPIRED는 회원·하루 1건 유니크라, 페이징용으로 서로 다른 날짜·타입을 쓴다.
		LocalDateTime day0 = LocalDateTime.now(clock).minusDays(2);
		LocalDateTime day1 = LocalDateTime.now(clock).minusDays(1);
		LocalDateTime day2 = LocalDateTime.now(clock);
		insertNotification(me, "EXPIRED", "첫번째", day0, null);
		insertNotification(me, "EXPIRED", "두번째", day1, null);
		insertNotification(me, "MEAL_PLAN_FAILED", "세번째", day2, null);

		mockMvc.perform(as(me, get(BASE).param("page", "0").param("size", "2")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(2))
				.andExpect(jsonPath("$.content[0].message").value("세번째"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.hasNext").value(true));

		mockMvc.perform(as(me, get(BASE).param("page", "1").param("size", "2")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].message").value("첫번째"))
				.andExpect(jsonPath("$.hasNext").value(false));
	}
}
