package com.remake.quota.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.remake.quota.application.port.out.RecipeRewardPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class RecipeRewardPersistenceAdapter implements RecipeRewardPort {
	private final NamedParameterJdbcTemplate jdbc;

	@Override
	public void grant(Long userId, Long postId, LocalDate weekStart, int rank, int amount,
			LocalDateTime now, LocalDateTime expiresAt) {
		jdbc.update("""
				INSERT INTO generation_quota_grants
					(user_id, shared_recipe_id, week_start, rank_no, amount, remaining, expires_at, created_at)
				VALUES (:user, :post, :week, :rank, :amount, :amount, :expiry, :now)
				""", params(userId, now).addValue("post", postId).addValue("week", weekStart)
					.addValue("rank", rank).addValue("amount", amount).addValue("expiry", expiresAt));
	}

	@Override
	public int remaining(Long userId, LocalDateTime now) {
		return jdbc.queryForObject("""
				SELECT COALESCE(SUM(remaining), 0) FROM generation_quota_grants
				WHERE user_id = :user AND remaining > 0 AND created_at <= :now AND expires_at > :now
				""", params(userId, now), Integer.class);
	}

	@Override
	public Optional<Long> consume(Long userId, LocalDateTime now) {
		return jdbc.query("""
				WITH next_grant AS (
					SELECT id FROM generation_quota_grants
					WHERE user_id = :user AND remaining > 0 AND created_at <= :now AND expires_at > :now
					ORDER BY expires_at, id LIMIT 1 FOR UPDATE
				)
				UPDATE generation_quota_grants g SET remaining = g.remaining - 1
				FROM next_grant n WHERE g.id = n.id RETURNING g.id
				""", params(userId, now), (rs, n) -> rs.getLong("id")).stream().findFirst();
	}

	private MapSqlParameterSource params(Long userId, LocalDateTime now) {
		return new MapSqlParameterSource("user", userId).addValue("now", now);
	}
}
