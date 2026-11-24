package com.remake.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.notification.application.port.in.DeleteUserNotificationsUseCase;
import com.remake.support.IntegrationTestSupport;

class DeleteUserNotificationsTest extends IntegrationTestSupport {

	private Long me;
	private Long other;

	@Autowired
	DeleteUserNotificationsUseCase deleteUserNotificationsUseCase;

	@BeforeEach
	void setUpUsers() {
		me = createUser();
		other = createUser();
	}

	private void insertNotification(Long userId, String type, String message, LocalDateTime createdAt,
			LocalDateTime readAt) {
		jdbcTemplate.update("""
				INSERT INTO notifications (user_id, type, message, read_at, created_at)
				VALUES (?, ?, ?, ?, ?)
				""", userId, type, message, readAt, createdAt);
	}

	private int countOf(Long userId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM notifications WHERE user_id = ?", Integer.class, userId);
		return count == null ? 0 : count;
	}

	@Test
	void 회원의_알림은_읽음_여부와_종류와_상관없이_모두_지워진다() {
		LocalDateTime now = LocalDateTime.now(clock);
		// EXPIRED는 회원·하루 1건 유니크라 날짜를 다르게 둔다.
		insertNotification(me, "EXPIRED", "안 읽은 기한 지남", now, null);
		insertNotification(me, "EXPIRED", "읽은 기한 지남", now.minusDays(1), now);
		insertNotification(me, "EXPIRY_SOON", "임박", now, null);
		insertNotification(me, "MEAL_PLAN_FAILED", "식단표 실패", now, now);
		assertThat(countOf(me)).isEqualTo(4);

		deleteUserNotificationsUseCase.deleteUserNotifications(me);

		assertThat(countOf(me)).isZero();
	}

	@Test
	void 다른_회원의_알림은_지워지지_않는다() {
		LocalDateTime now = LocalDateTime.now(clock);
		insertNotification(me, "EXPIRY_SOON", "내 알림", now, null);
		insertNotification(other, "EXPIRY_SOON", "남의 안 읽은 알림", now, null);
		insertNotification(other, "EXPIRED", "남의 읽은 알림", now, now);

		deleteUserNotificationsUseCase.deleteUserNotifications(me);

		assertThat(countOf(me)).isZero();
		assertThat(countOf(other)).isEqualTo(2);
		List<String> messages = jdbcTemplate.queryForList(
				"SELECT message FROM notifications WHERE user_id = ? ORDER BY id", String.class, other);
		assertThat(messages).containsExactly("남의 안 읽은 알림", "남의 읽은 알림");
	}

	@Test
	void 알림이_없는_회원도_오류_없이_끝난다() {
		assertThat(countOf(me)).isZero();

		assertThatCode(() -> deleteUserNotificationsUseCase.deleteUserNotifications(me))
				.doesNotThrowAnyException();

		assertThat(countOf(me)).isZero();
	}
}
