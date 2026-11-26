package com.remake.share.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.remake.share.application.port.out.WeeklyRankingSettlementPort;
import com.remake.share.domain.SharedRecipeRankingPolicy;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class WeeklyRankingSettlementPersistenceAdapter implements WeeklyRankingSettlementPort {
	private final NamedParameterJdbcTemplate jdbc;

	@Override
	public boolean claim(LocalDate weekStart, LocalDateTime now) {
		return jdbc.update("""
				INSERT INTO shared_recipe_weekly_settlements (week_start, created_at)
				VALUES (:week, :now) ON CONFLICT (week_start) DO NOTHING
				""", new MapSqlParameterSource("week", weekStart).addValue("now", now)) == 1;
	}

	@Override
	public List<Rank> rank(LocalDateTime start, LocalDateTime end) {
		// #88과 동일한 제안: 본인 저장 제외, 동점은 먼저 공유한 글·ID 순서로 단독 순위 부여.
		return jdbc.query("""
				SELECT p.id, r.user_id, COUNT(s.id) AS save_count,
					ROW_NUMBER() OVER (ORDER BY COUNT(s.id) DESC, p.created_at ASC, p.id ASC) AS rank_no
				FROM shared_recipes p
				JOIN recipes r ON r.id = p.recipe_id
				JOIN saved_recipes s ON s.shared_recipe_id = p.id
					AND s.saved_at >= :start AND s.saved_at < :end
					AND (NOT :excludeOwn OR s.user_id <> r.user_id)
				WHERE p.status = 'VISIBLE'
				GROUP BY p.id, r.user_id
				ORDER BY rank_no LIMIT :limit
				""", new MapSqlParameterSource("start", start).addValue("end", end)
					.addValue("excludeOwn", SharedRecipeRankingPolicy.EXCLUDE_OWN_SAVES)
					.addValue("limit", SharedRecipeRankingPolicy.POPULAR_LIMIT),
				(rs, n) -> new Rank(rs.getLong("id"), rs.getLong("user_id"),
					rs.getInt("rank_no"), rs.getInt("save_count")));
	}

	@Override
	public void save(LocalDate weekStart, Rank rank, LocalDateTime now) {
		jdbc.update("""
				INSERT INTO shared_recipe_weekly_ranks (week_start, shared_recipe_id, rank_no, save_count, created_at)
				VALUES (:week, :post, :rank, :count, :now)
				""", new MapSqlParameterSource("week", weekStart).addValue("post", rank.sharedRecipeId())
					.addValue("rank", rank.rank()).addValue("count", rank.saveCount()).addValue("now", now));
	}
}
