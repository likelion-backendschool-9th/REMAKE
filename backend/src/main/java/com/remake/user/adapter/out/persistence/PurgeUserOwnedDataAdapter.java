package com.remake.user.adapter.out.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.remake.user.application.port.out.PurgeUserOwnedDataPort;

import lombok.RequiredArgsConstructor;

/**
 * 탈퇴 시 회원 소유 행을 FK 순서에 맞게 지운다.
 * 레시피·공유·이벤트는 유지. 알림은 {@code DeleteUserNotificationsUseCase}에서 지운다.
 * 구독 테이블은 아직 없으면 건드리지 않는다.
 */
@Component
@RequiredArgsConstructor
class PurgeUserOwnedDataAdapter implements PurgeUserOwnedDataPort {

	private final JdbcTemplate jdbcTemplate;

	@Override
	public void purge(Long userId) {
		// meal_plan_slots는 meal_plans ON DELETE CASCADE
		jdbcTemplate.update("DELETE FROM cart_items WHERE user_id = ?", userId);
		jdbcTemplate.update("DELETE FROM meal_plans WHERE user_id = ?", userId);
		jdbcTemplate.update("DELETE FROM saved_recipes WHERE user_id = ?", userId);
		jdbcTemplate.update("DELETE FROM ingredient_logs WHERE user_id = ?", userId);
		jdbcTemplate.update("DELETE FROM ingredients WHERE user_id = ?", userId);
		// receipt_items는 receipts ON DELETE CASCADE. ingredients.receipt_item_id는 SET NULL
		jdbcTemplate.update("DELETE FROM receipts WHERE user_id = ?", userId);
		jdbcTemplate.update("DELETE FROM usage_logs WHERE user_id = ?", userId);
		jdbcTemplate.update("DELETE FROM user_excluded_ingredients WHERE user_id = ?", userId);
	}
}
