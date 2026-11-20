package com.remake.event.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.domain.EventName;
import com.remake.event.domain.EventRecord;
import com.remake.event.domain.Screen;

class EventTrackerEnqueueFailureTest {

	@Test
	void 비동기_제출이_실패해도_track은_예외를_던지지_않는다() {
		EventWriteService writeService = mock(EventWriteService.class);
		doThrow(new RejectedExecutionException("queue full")).when(writeService).saveAsync(any(EventRecord.class));
		EventTrackerService tracker = new EventTrackerService(
				writeService,
				Clock.fixed(Instant.parse("2026-03-04T01:00:00Z"), ZoneId.of("Asia/Seoul")));

		assertThatCode(() -> tracker.track(
				EventName.RECEIPT_SAVED,
				Screen.RECEIPT_RESULT,
				Map.of(
						"receipt_id", 1,
						"registered_count", 1,
						"ocr_item_count", 1,
						"edited_item_count", 0,
						"deleted_item_count", 0,
						"added_item_count", 0,
						"duration_sec", 1,
						"food_total_amount", 100),
				EventTrackCommand.ofUser(1L))).doesNotThrowAnyException();
	}

	@Test
	void afterCommit에서_제출이_실패해도_예외를_밖으로_던지지_않는다() {
		EventWriteService writeService = mock(EventWriteService.class);
		doThrow(new RejectedExecutionException("queue full")).when(writeService).saveAsync(any(EventRecord.class));
		EventTrackerService tracker = new EventTrackerService(
				writeService,
				Clock.fixed(Instant.parse("2026-03-04T01:00:00Z"), ZoneId.of("Asia/Seoul")));

		TransactionSynchronizationManager.initSynchronization();
		try {
			assertThatCode(() -> tracker.track(
					EventName.LOGIN_SUCCEEDED,
					Screen.LOGIN,
					Map.of("provider", "kakao", "is_new_user", false),
					EventTrackCommand.ofUser(2L))).doesNotThrowAnyException();

			assertThatCode(() -> {
				for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
					sync.afterCommit();
				}
			}).doesNotThrowAnyException();
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}
}
