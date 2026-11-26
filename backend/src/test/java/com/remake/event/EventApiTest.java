package com.remake.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.remake.event.adapter.in.web.EventController;
import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.support.IntegrationTestSupport;

class EventApiTest extends IntegrationTestSupport {

	@Test
	void 클라이언트_이벤트를_수집하고_모르는_이름은_거절한다() throws Exception {
		Long userId = createUser();
		String body = """
				{
				  "events": [
				    {
				      "name": "app_opened",
				      "screen": "login",
				      "sessionId": "s-1",
				      "occurredAt": "2026-03-04T10:00:00+09:00",
				      "properties": { "entry_source": "direct", "is_standalone": true, "email": "x@y.com" }
				    },
				    { "name": "not_a_real_event", "screen": "login", "properties": {} },
				    { "name": "login_succeeded", "screen": "login", "properties": { "provider": "kakao" } }
				  ]
				}
				""";

		mockMvc.perform(post("/api/v1/user/events")
						.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId)
						.contentType(MediaType.APPLICATION_JSON)
						.header(EventController.HEADER_PLATFORM, "PWA")
						.header(EventController.HEADER_APP_VERSION, "1.0.0")
						.content(body))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.accepted").value(1))
				.andExpect(jsonPath("$.rejected").value(2));

		Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events", Integer.class);
		assertThat(count).isEqualTo(1);
		String name = jdbcTemplate.queryForObject("SELECT event_name FROM events", String.class);
		assertThat(name).isEqualTo("app_opened");
		String props = jdbcTemplate.queryForObject("SELECT properties::text FROM events", String.class);
		assertThat(props).contains("entry_source").doesNotContain("email");
		assertThat(jdbcTemplate.queryForObject("SELECT platform FROM events", String.class)).isEqualTo("PWA");
		assertThat(jdbcTemplate.queryForObject("SELECT source FROM events", String.class)).isEqualTo("CLIENT");
		assertThat(jdbcTemplate.queryForObject("SELECT user_id FROM events", Long.class)).isEqualTo(userId);
	}

	@Test
	void null_속성_이벤트만_거절하고_같은_배치의_정상_이벤트는_저장한다() throws Exception {
		String body = """
				{
				  "events": [
				    {
				      "name": "app_opened",
				      "screen": "login",
				      "properties": { "entry_source": null, "is_standalone": true }
				    },
				    {
				      "name": "app_opened",
				      "screen": "login",
				      "properties": { "entry_source": "direct", "is_standalone": false }
				    }
				  ]
				}
				""";

		mockMvc.perform(post("/api/v1/user/events")
						.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, createUser())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.accepted").value(1))
				.andExpect(jsonPath("$.rejected").value(1));

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT properties->>'entry_source' FROM events", String.class))
				.isEqualTo("direct");
	}

	@Test
	void 허용_속성에_자유_입력이_오면_거절한다() throws Exception {
		String body = """
				{
				  "events": [
				    {
				      "name": "app_opened",
				      "screen": "login",
				      "properties": { "entry_source": "leak@example.com", "is_standalone": true }
				    },
				    {
				      "name": "app_opened",
				      "screen": "login",
				      "properties": { "entry_source": "direct", "is_standalone": true }
				    }
				  ]
				}
				""";

		mockMvc.perform(post("/api/v1/user/events")
						.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, createUser())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.accepted").value(1))
				.andExpect(jsonPath("$.rejected").value(1));
	}

	@Test
	void 배치가_50개를_넘으면_EVENT_BATCH_TOO_LARGE() throws Exception {
		String events = IntStream.range(0, 51)
				.mapToObj(i -> """
						{"name":"app_opened","screen":"login","properties":{"entry_source":"direct","is_standalone":false}}
						""")
				.collect(Collectors.joining(","));

		mockMvc.perform(post("/api/v1/user/events")
						.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, createUser())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"events\":[" + events + "]}"))
				.andExpect(status().isContentTooLarge())
				.andExpect(jsonPath("$.code").value("EVENT_BATCH_TOO_LARGE"));
	}
}
