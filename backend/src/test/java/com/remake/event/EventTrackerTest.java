package com.remake.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.support.IntegrationTestSupport;

class EventTrackerTest extends IntegrationTestSupport {

	@Autowired
	private EventTracker eventTracker;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Test
	void 화면없는_배치_이벤트는_screen을_null로_저장한다() {
		transactionTemplate.executeWithoutResult(status -> eventTracker.track(
				EventName.WEEKLY_REWARD_GRANTED, null,
				Map.of("reward_rank", 1, "reward_amount", 10), EventTrackCommand.ofUser(1L)));
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT screen FROM events", String.class)).isNull();
	}

	@Test
	void 커밋_후에만_서버_이벤트가_쌓인다() {
		transactionTemplate.executeWithoutResult(status -> eventTracker.track(
				EventName.RECEIPT_SAVED,
				Screen.RECEIPT_RESULT,
				Map.of(
						"receipt_id", 10,
						"registered_count", 2,
						"ocr_item_count", 3,
						"edited_item_count", 0,
						"deleted_item_count", 0,
						"added_item_count", 0,
						"duration_sec", 12,
						"food_total_amount", 1000),
				EventTrackCommand.of(1L, "sess", "PWA", "1.0.0")));

		awaitEventWrites();

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT source FROM events", String.class)).isEqualTo("SERVER");
		assertThat(jdbcTemplate.queryForObject("SELECT event_name FROM events", String.class)).isEqualTo("receipt_saved");
		assertThat(jdbcTemplate.queryForObject("SELECT user_id FROM events", Long.class)).isEqualTo(1L);
	}

	@Test
	void 롤백되면_서버_이벤트를_남기지_않는다() {
		try {
			transactionTemplate.executeWithoutResult(status -> {
				eventTracker.track(
						EventName.LOGIN_SUCCEEDED,
						Screen.LOGIN,
						Map.of("provider", "kakao", "is_new_user", true),
						EventTrackCommand.ofUser(99L));
				status.setRollbackOnly();
			});
		}
		catch (Exception ignored) {
			// rollback-only는 예외 없이 끝난다
		}

		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events", Integer.class)).isZero();
	}

	@Test
	void 금지_속성이_있어도_요청은_깨지지_않고_허용_값만_남긴다() {
		eventTracker.track(
				EventName.LOGIN_FAILED,
				Screen.LOGIN,
				Map.of("provider", "google", "error_code", "OAUTH_FAILED", "email", "a@b.com"),
				EventTrackCommand.ofUser(7L));

		awaitEventWrites();

		String props = jdbcTemplate.queryForObject("SELECT properties::text FROM events", String.class);
		assertThat(props).contains("provider").contains("error_code").doesNotContain("email");
	}
}
