package com.remake.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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

	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	private Long onUser;
	private Long offUser;

	@Autowired
	RunExpiryNotificationBatchUseCase runExpiryNotificationBatchUseCase;

	@Autowired
	CleanupOldNotificationsUseCase cleanupOldNotificationsUseCase;

	@BeforeEach
	void setUp() {
		onUser = createUser();
		offUser = createUser();
		jdbcTemplate.update("UPDATE users SET notification_enabled = false WHERE id = ?", offUser);
	}

	@Test
	void 임박은_재료마다_기한_지남은_하루_한_건이고_끈_회원은_제외한다() {
		createIngredient(onUser, "DAIRY_EGG", "우유", TODAY.plusDays(1));
		createIngredient(onUser, "FRUIT", "사과", TODAY);
		createIngredient(onUser, "MEAT", "지난고기", TODAY.minusDays(2));
		createIngredient(onUser, "MEAT", "어제고기", TODAY.minusDays(1));
		createIngredient(offUser, "FRUIT", "끈회원사과", TODAY);
		createIngredient(offUser, "MEAT", "끈회원만료", TODAY.minusDays(1));

		runExpiryNotificationBatchUseCase.runExpiryNotificationBatch();
		awaitEventWrites();

		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"SELECT type, message, ingredient_id FROM notifications WHERE user_id = ? ORDER BY type, id",
				onUser);
		assertThat(rows).hasSize(3);
		assertThat(rows.stream().filter(r -> "EXPIRY_SOON".equals(r.get("type")))).hasSize(2);
		assertThat(rows.stream().filter(r -> "EXPIRED".equals(r.get("type")))).hasSize(1);
		assertThat(rows.stream().filter(r -> "EXPIRED".equals(r.get("type"))).findFirst().orElseThrow()
				.get("message").toString()).contains("2개");

		Integer offCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM notifications WHERE user_id = ?", Integer.class, offUser);
		assertThat(offCount).isZero();

		Integer sent = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'notification_sent' AND user_id = ?",
				Integer.class, onUser);
		assertThat(sent).isEqualTo(3);

		Integer expiredEvents = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'ingredient_expired' AND user_id = ?",
				Integer.class, onUser);
		assertThat(expiredEvents).isEqualTo(1);
	}

	@Test
	void 같은_날_두_번_돌아도_알림과_이벤트가_늘지_않는다() {
		createIngredient(onUser, "FRUIT", "사과", TODAY.plusDays(2));
		createIngredient(onUser, "MEAT", "어제고기", TODAY.minusDays(1));

		runExpiryNotificationBatchUseCase.runExpiryNotificationBatch();
		awaitEventWrites();
		runExpiryNotificationBatchUseCase.runExpiryNotificationBatch();
		awaitEventWrites();

		Integer notifications = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM notifications WHERE user_id = ?", Integer.class, onUser);
		assertThat(notifications).isEqualTo(2);

		Integer sent = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'notification_sent' AND user_id = ?",
				Integer.class, onUser);
		assertThat(sent).isEqualTo(2);

		Integer expiredEvents = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'ingredient_expired' AND user_id = ?",
				Integer.class, onUser);
		assertThat(expiredEvents).isEqualTo(1);
	}

	@Test
	void 알림_이벤트_속성은_명세대로_들어가고_명세에_없는_속성은_없다() {
		createIngredient(onUser, "DAIRY_EGG", "우유", TODAY.plusDays(1));
		createIngredient(onUser, "MEAT", "어제고기", TODAY.minusDays(1), 3000L);
		createIngredient(onUser, "FRUIT", "어제사과", TODAY.minusDays(1));

		runExpiryNotificationBatchUseCase.runExpiryNotificationBatch();
		awaitEventWrites();

		List<Map<String, Object>> sent = jdbcTemplate.queryForList("""
				SELECT properties ->> 'notification_type' AS notification_type,
				       properties ->> 'batch_count' AS batch_count
				FROM events
				WHERE event_name = 'notification_sent' AND user_id = ?
				ORDER BY notification_type
				""", onUser);
		assertThat(sent)
				.extracting(row -> row.get("notification_type"), row -> row.get("batch_count"))
				.containsExactly(tuple("expired", "2"), tuple("expiry_soon", "1"));

		List<Map<String, Object>> expired = jdbcTemplate.queryForList("""
				SELECT properties ->> 'category_code' AS category_code,
				       properties ->> 'has_price' AS has_price
				FROM events
				WHERE event_name = 'ingredient_expired' AND user_id = ?
				ORDER BY category_code
				""", onUser);
		assertThat(expired)
				.extracting(row -> row.get("category_code"), row -> row.get("has_price"))
				.containsExactly(tuple("FRUIT", "false"), tuple("MEAT", "true"));

		assertThat(propertyKeys("notification_sent"))
				.containsExactlyInAnyOrder("notification_type", "batch_count");
		assertThat(propertyKeys("ingredient_expired"))
				.containsExactlyInAnyOrder("category_code", "has_price");
	}

	private List<String> propertyKeys(String eventName) {
		return jdbcTemplate.queryForList(
				"SELECT DISTINCT jsonb_object_keys(properties) FROM events WHERE event_name = ?",
				String.class, eventName);
	}

	@Test
	void 삼십일_지난_알림은_정리된다() {
		LocalDateTime old = TODAY.minusDays(31).atStartOfDay();
		LocalDateTime recent = TODAY.minusDays(10).atStartOfDay();
		jdbcTemplate.update("""
				INSERT INTO notifications (user_id, type, message, created_at)
				VALUES (?, 'EXPIRED', '지울것', ?), (?, 'EXPIRED', '남길것', ?)
				""", onUser, old, onUser, recent);

		int deleted = cleanupOldNotificationsUseCase.cleanupOldNotifications();
		assertThat(deleted).isEqualTo(1);

		Integer left = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM notifications WHERE user_id = ?", Integer.class, onUser);
		assertThat(left).isEqualTo(1);
		String message = jdbcTemplate.queryForObject(
				"SELECT message FROM notifications WHERE user_id = ?", String.class, onUser);
		assertThat(message).isEqualTo("남길것");
	}

	@Test
	void 임박은_D3까지_포함하고_D4는_제외한다() {
		createIngredient(onUser, "FRUIT", "D3사과", TODAY.plusDays(3));
		createIngredient(onUser, "FRUIT", "D4배", TODAY.plusDays(4));

		runExpiryNotificationBatchUseCase.runExpiryNotificationBatch();

		List<Map<String, Object>> soon = jdbcTemplate.queryForList(
				"SELECT message FROM notifications WHERE user_id = ? AND type = 'EXPIRY_SOON'",
				onUser);
		assertThat(soon).hasSize(1);
		assertThat(soon.get(0).get("message").toString()).contains("D3사과");
	}
}
