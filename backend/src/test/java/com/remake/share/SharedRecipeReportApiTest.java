package com.remake.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.share.application.port.in.PublishSharedRecipeUseCase;
import com.remake.share.application.port.in.SharedRecipePhotoUpload;
import com.remake.share.domain.SharedRecipeReportReason;
import com.remake.support.IntegrationTestSupport;

class SharedRecipeReportApiTest extends IntegrationTestSupport {
	@Autowired private PublishSharedRecipeUseCase publish;
	private Long shared() {
		Long author = createUser(); byte[] jpeg = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 1};
		return publish.publish(author, createRecipe(author), "공유 제목", new SharedRecipePhotoUpload(
				"image/jpeg", jpeg.length, () -> new ByteArrayInputStream(jpeg)));
	}
	private MockHttpServletRequestBuilder request(Long id, Long user, String body) {
		var request = post("/api/v1/user/shared-recipes/" + id + "/reports")
				.contentType(MediaType.APPLICATION_JSON).content(body);
		return user == null ? request : request.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user);
	}
	private int report(Long id, Long user) throws Exception {
		return mockMvc.perform(request(id, user, "{\"reason\":\"OTHER\"}"))
				.andReturn().getResponse().getStatus();
	}

	@ParameterizedTest
	@EnumSource(SharedRecipeReportReason.class)
	void 선택한_네사유를_저장하고_정의서의_소문자_이벤트로_발행한다(SharedRecipeReportReason reason) throws Exception {
		Long id = shared(); Long user = createUser();
		mockMvc.perform(request(id, user, "{\"reason\":\"" + reason.name() + "\"}"))
				.andExpect(status().isCreated());
		var row = jdbcTemplate.queryForMap("SELECT * FROM shared_recipe_reports");
		assertThat(row.get("shared_recipe_id")).isEqualTo(id);
		assertThat(row.get("reporter_id")).isEqualTo(user);
		assertThat(row.get("reason")).isEqualTo(reason.name());
		assertThat(row.get("status")).isEqualTo("PENDING");
		awaitEventWrites();
		var event = jdbcTemplate.queryForMap("SELECT user_id, properties::text AS props FROM events WHERE event_name = 'shared_recipe_reported'");
		assertThat(event.get("user_id")).isEqualTo(user);
		assertThat(event.get("props").toString()).contains("shared_recipe_id", reason.eventValue())
				.doesNotContain("공유 제목", "email", "reasonDetail");
		assertThat(jdbcTemplate.queryForObject("SELECT properties ->> 'report_reason' FROM events WHERE event_name = 'shared_recipe_reported'", String.class))
				.isEqualTo(reason.eventValue());
		assertThat(jdbcTemplate.queryForObject("SELECT status FROM shared_recipes WHERE id = ?", String.class, id)).isEqualTo("VISIBLE");
	}

	@Test
	void 사유를_바꾸어도_같은회원은_같은글을_한번만_신고한다() throws Exception {
		Long id = shared(); Long user = createUser();
		assertThat(report(id, user)).isEqualTo(201);
		mockMvc.perform(request(id, user, "{\"reason\":\"AD_TITLE\"}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REPORT_ALREADY_EXISTS"));
		assertThat(jdbcTemplate.queryForObject("SELECT reason FROM shared_recipe_reports", String.class)).isEqualTo("OTHER");
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events WHERE event_name = 'shared_recipe_reported'", Long.class)).isEqualTo(1);
	}

	@Test
	void 동시신고도_한건만_저장하고_성공이벤트도_한번이다() throws Exception {
		Long id = shared(); Long user = createUser(); CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			Callable<Integer> action = () -> { start.await(); return report(id, user); };
			var first = executor.submit(action); var second = executor.submit(action); start.countDown();
			assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
					.containsExactlyInAnyOrder(201, 409);
		}
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM shared_recipe_reports", Long.class)).isEqualTo(1);
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events WHERE event_name = 'shared_recipe_reported'", Long.class)).isEqualTo(1);
	}

	@Test
	void 다른회원의_신고는_허용하지만_자동으로_숨기지않는다() throws Exception {
		Long id = shared();
		for (int i = 0; i < 4; i++) { assertThat(report(id, createUser())).isEqualTo(201); }
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM shared_recipe_reports", Long.class)).isEqualTo(4);
		assertThat(jdbcTemplate.queryForObject("SELECT status FROM shared_recipes WHERE id = ?", String.class, id)).isEqualTo("VISIBLE");
	}

	@Test
	void 사유_누락과_옛사유와_자유입력은_거절한다() throws Exception {
		Long id = shared(); Long user = createUser();
		for (String body : List.of("{}", "{\"reason\":null}", "{\"reason\":\"PHOTO\"}",
				"{\"reason\":\"other\"}", "{\"reason\":\"OTHER\",\"reasonDetail\":\"자유 입력\"}")) {
			mockMvc.perform(request(id, user, body)).andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		}
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM shared_recipe_reports", Long.class)).isZero();
	}

	@Test
	void 비로그인은_401이고_숨김과_없는글은_404다() throws Exception {
		Long id = shared(); Long user = createUser();
		mockMvc.perform(request(id, null, "{\"reason\":\"OTHER\"}")).andExpect(status().isUnauthorized());
		jdbcTemplate.update("UPDATE shared_recipes SET status = 'HIDDEN' WHERE id = ?", id);
		for (Long target : new Long[]{id, 999999L}) {
			mockMvc.perform(request(target, user, "{\"reason\":\"OTHER\"}"))
					.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SHARED_RECIPE_NOT_FOUND"));
		}
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM shared_recipe_reports", Long.class)).isZero();
	}

	@Test
	void DB저장실패시_신고와_성공이벤트가_남지않는다() throws Exception {
		Long id = shared(); Long user = createUser();
		jdbcTemplate.execute("ALTER TABLE shared_recipe_reports ADD CONSTRAINT test_reject_report CHECK (reason <> 'OTHER')");
		try {
			mockMvc.perform(request(id, user, "{\"reason\":\"OTHER\"}"))
					.andExpect(status().isInternalServerError());
			assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM shared_recipe_reports", Long.class)).isZero();
			awaitEventWrites();
			assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events WHERE event_name = 'shared_recipe_reported'", Long.class)).isZero();
		} finally {
			jdbcTemplate.execute("ALTER TABLE shared_recipe_reports DROP CONSTRAINT test_reject_report");
		}
	}
}
