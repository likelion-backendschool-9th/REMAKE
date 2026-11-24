package com.remake.user.adapter.out.persistence;

import java.io.IOException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.remake.receipt.application.port.out.ReceiptImageStoragePort;
import com.remake.user.application.port.out.PurgeUserOwnedDataPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 탈퇴 시 회원 소유 행을 FK 순서에 맞게 지운다.
 * 레시피·공유·이벤트는 유지. 알림은 {@code DeleteUserNotificationsUseCase}에서 지운다.
 * 구독 테이블은 아직 없으면 건드리지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class PurgeUserOwnedDataAdapter implements PurgeUserOwnedDataPort {

	private final JdbcTemplate jdbcTemplate;
	private final ReceiptImageStoragePort receiptImageStorage;

	@Override
	public void purge(Long userId) {
		// meal_plan_slots는 meal_plans ON DELETE CASCADE
		jdbcTemplate.update("DELETE FROM cart_items WHERE user_id = ?", userId);
		jdbcTemplate.update("DELETE FROM meal_plans WHERE user_id = ?", userId);
		jdbcTemplate.update("DELETE FROM saved_recipes WHERE user_id = ?", userId);
		jdbcTemplate.update("DELETE FROM ingredient_logs WHERE user_id = ?", userId);
		jdbcTemplate.update("DELETE FROM ingredients WHERE user_id = ?", userId);

		List<String> receiptImageKeys = jdbcTemplate.queryForList(
				"SELECT image_key FROM receipts WHERE user_id = ?", String.class, userId);
		// receipt_items는 receipts ON DELETE CASCADE. ingredients.receipt_item_id는 SET NULL
		jdbcTemplate.update("DELETE FROM receipts WHERE user_id = ?", userId);
		deleteReceiptImages(receiptImageKeys);

		jdbcTemplate.update("DELETE FROM usage_logs WHERE user_id = ?", userId);
		jdbcTemplate.update("DELETE FROM user_excluded_ingredients WHERE user_id = ?", userId);
	}

	private void deleteReceiptImages(List<String> imageKeys) {
		for (String imageKey : imageKeys) {
			if (imageKey == null || imageKey.isBlank()) {
				continue;
			}
			try {
				receiptImageStorage.delete(imageKey);
			} catch (IOException | RuntimeException e) {
				// DB 행은 이미 지웠다. 파일만 남은 경우는 6개월 정리·재시도 작업이 맡는다.
				log.warn("탈퇴 영수증 사진 삭제 실패 key={}", imageKey, e);
			}
		}
	}
}
