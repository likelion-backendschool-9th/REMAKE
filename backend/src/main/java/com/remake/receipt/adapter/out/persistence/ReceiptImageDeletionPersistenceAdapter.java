package com.remake.receipt.adapter.out.persistence;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.remake.receipt.application.port.out.ReceiptImageDeletionPort;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
class ReceiptImageDeletionPersistenceAdapter implements ReceiptImageDeletionPort {

	private final JdbcClient jdbc;
	private final Clock clock;

	@Override
	public void enqueue(String imageKey) {
		jdbc.sql("INSERT INTO receipt_image_deletions (image_key, requested_at) VALUES (?, ?)")
				.param(imageKey).param(LocalDateTime.now(clock)).update();
	}

	@Override
	public boolean lock(String imageKey) {
		return jdbc.sql("""
				SELECT image_key FROM receipt_image_deletions WHERE image_key = ? FOR UPDATE SKIP LOCKED
				""").param(imageKey).query(String.class).optional().isPresent();
	}

	@Override
	public List<String> lockPending() {
		return jdbc.sql("""
				SELECT image_key FROM receipt_image_deletions
				ORDER BY requested_at, image_key LIMIT 50 FOR UPDATE SKIP LOCKED
				""").query(String.class).list();
	}

	@Override
	public void complete(String imageKey) {
		jdbc.sql("DELETE FROM receipt_image_deletions WHERE image_key = ?").param(imageKey).update();
	}

	@Override
	public void defer(String imageKey) {
		jdbc.sql("UPDATE receipt_image_deletions SET requested_at = ? WHERE image_key = ?")
				.param(LocalDateTime.now(clock)).param(imageKey).update();
	}
}
