package com.remake.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.notification.application.port.in.CleanupOldNotificationsUseCase;
import com.remake.notification.application.port.in.RunExpiryNotificationBatchUseCase;
import com.remake.support.IntegrationTestSupport;

class ExpiryNotificationBatchTest extends IntegrationTestSupport {

	private static final Long ON_USER = 301L;
	private static final Long OFF_USER = 302L;
	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	@Autowired
	RunExpiryNotificationBatchUseCase runExpiryNotificationBatchUseCase;

	@Autowired
	CleanupOldNotificationsUseCase cleanupOldNotificationsUseCase;

	@BeforeEach
	void setUp() {
		ensureUser(ON_USER);
		ensureUser(OFF_USER);
		jdbcTemplate.update("UPDATE users SET notification_enabled = false WHERE id = ?", OFF_USER);
	}

	@Test
	void 임박은_재료마다_기한_지남은_하루_한_건이고_끈_회원은_제외한다() {
		createIngredient(ON_USER, "DAIRY_EGG", "우유", TODAY.plusDays(1));
		createIngredient(ON_USER, "FRUIT", "사과", TODAY);
		createIngredient(ON_USER, "MEAT", "지난고기", TODAY.minusDays(2));
		createIngredient(ON_USER, "MEAT", "어제고기", TODAY.minusDays(1));
		createIngredient(OFF_USER, "FRUIT", "끈회원사과", TODAY);
		createIngredient(OFF_USER, "MEAT", "끈회원만료", TODAY.minusDays(1));

		runExpiryNotificationBatchUseCase.runExpiryNotificationBatch();
		awaitEventWrites();

		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"SELECT type, message, ingredient_id FROM notifications WHERE user_id = ? ORDER BY type, id",
				ON_USER);
		assertThat(rows).hasSize(3);
		assertThat(rows.stream().filter(r -> "EXPIRY_SOON".equals(r.get("type")))).hasSize(2);
		assertThat(rows.stream().filter(r -> "EXPIRED".equals(r.get("type")))).hasSize(1);
		assertThat(rows.stream().filter(r -> "EXPIRED".equals(r.get("type"))).findFirst().orElseThrow()
				.get("message").toString()).contains("2개");

		Integer offCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM notifications WHERE user_id = ?", Integer.class, OFF_USER);
		assertThat(offCount).isZero();

		Integer sent = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'notification_sent' AND user_id = ?",
				Integer.class, ON_USER);
		assertThat(sent).isEqualTo(3);

		Integer expiredEvents = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'ingredient_expired' AND user_id = ?",
				Integer.class, ON_USER);
		assertThat(expiredEvents).isEqualTo(1);
	}

	@Test
	void 같은_날_두_번_돌아도_알림과_이벤트가_늘지_않는다() {
		createIngredient(ON_USER, "FRUIT", "사과", TODAY.plusDays(2));
		createIngredient(ON_USER, "MEAT", "어제고기", TODAY.minusDays(1));

		runExpiryNotificationBatchUseCase.runExpiryNotificationBatch();
		awaitEventWrites();
		runExpiryNotificationBatchUseCase.runExpiryNotificationBatch();
		awaitEventWrites();

		Integer notifications = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM notifications WHERE user_id = ?", Integer.class, ON_USER);
		assertThat(notifications).isEqualTo(2);

		Integer sent = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'notification_sent' AND user_id = ?",
				Integer.class, ON_USER);
		assertThat(sent).isEqualTo(2);

		Integer expiredEvents = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'ingredient_expired' AND user_id = ?",
				Integer.class, ON_USER);
		assertThat(expiredEvents).isEqualTo(1);
	}

	@Test
	void 삼십일_지난_알림은_정리된다() {
		LocalDateTime old = TODAY.minusDays(31).atStartOfDay();
		LocalDateTime recent = TODAY.minusDays(10).atStartOfDay();
		jdbcTemplate.update("""
				INSERT INTO notifications (user_id, type, message, created_at)
				VALUES (?, 'EXPIRED', '지울것', ?), (?, 'EXPIRED', '남길것', ?)
				""", ON_USER, old, ON_USER, recent);

		int deleted = cleanupOldNotificationsUseCase.cleanupOldNotifications();
		assertThat(deleted).isEqualTo(1);

		Integer left = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM notifications WHERE user_id = ?", Integer.class, ON_USER);
		assertThat(left).isEqualTo(1);
		String message = jdbcTemplate.queryForObject(
				"SELECT message FROM notifications WHERE user_id = ?", String.class, ON_USER);
		assertThat(message).isEqualTo("남길것");
	}
}
