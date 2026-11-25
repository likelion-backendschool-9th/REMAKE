package com.remake.share.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.remake.share.application.port.out.LoadSharedRecipeRankingPort;
import com.remake.share.domain.SharedRecipeRankingPolicy;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class SharedRecipeRankingPersistenceAdapter implements LoadSharedRecipeRankingPort {
	/** 제안: 저장 수 동점은 먼저 공유한 글 우선. 같은 공유 시각은 ID로 순서를 고정한다. */
	private static final String POPULAR_ORDER = "weekly_save_count DESC, created_at ASC, id ASC";
	private static final String POPULAR = """
			WITH counts AS (
				SELECT p.id, p.recipe_id, p.title, p.created_at, COUNT(s.id) AS weekly_save_count
				FROM shared_recipes p
				JOIN recipes r ON r.id = p.recipe_id
				LEFT JOIN saved_recipes s ON s.shared_recipe_id = p.id
					AND s.saved_at >= :weekStart AND s.saved_at <= :now
					AND (NOT :excludeOwn OR s.user_id <> r.user_id)
				WHERE p.status = 'VISIBLE'
				GROUP BY p.id
				HAVING COUNT(s.id) > 0
			), ranked AS (
				SELECT counts.*, ROW_NUMBER() OVER (ORDER BY %s) AS rank_no FROM counts
			)
			""".formatted(POPULAR_ORDER);
	private static final String SEARCH = "strpos(lower(title), lower(:query)) > 0";
	private final NamedParameterJdbcTemplate jdbc;

	@Override
	public Page<Row> popular(LocalDateTime weekStart, LocalDateTime now, String query, int page, int size) {
		var params = parameters(query, page, size).addValue("weekStart", weekStart).addValue("now", now)
				.addValue("previousWeek", weekStart.toLocalDate().minusWeeks(1))
				.addValue("excludeOwn", SharedRecipeRankingPolicy.EXCLUDE_OWN_SAVES)
				.addValue("limit", SharedRecipeRankingPolicy.POPULAR_LIMIT);
		// TOP 100과 전체 순위를 먼저 정하고 검색한다. 검색·페이지가 바뀌어도 순위는 같다.
		String selected = "SELECT * FROM ranked WHERE rank_no <= :limit AND " + SEARCH;
		Long total = jdbc.queryForObject(POPULAR + "SELECT COUNT(*) FROM (" + selected + ") matched",
				params, Long.class);
		var rows = jdbc.query(POPULAR + """
				SELECT p.*, previous.rank_no AS previous_rank FROM (
				""" + selected + """
				) p LEFT JOIN shared_recipe_weekly_ranks previous
					ON previous.shared_recipe_id = p.id AND previous.week_start = :previousWeek
				ORDER BY p.rank_no LIMIT :size OFFSET :offset
				""", params, (rs, number) -> row(rs, rs.getInt("rank_no"), rs.getObject("previous_rank", Integer.class)));
		return new PageImpl<>(rows, PageRequest.of(page, size), total);
	}

	@Override
	public Page<Row> latest(String query, int page, int size) {
		var params = parameters(query, page, size);
		String from = " FROM shared_recipes WHERE status = 'VISIBLE' AND " + SEARCH;
		Long total = jdbc.queryForObject("SELECT COUNT(*)" + from, params, Long.class);
		var rows = jdbc.query("SELECT id, recipe_id, title, created_at" + from
				+ " ORDER BY created_at DESC, id DESC LIMIT :size OFFSET :offset", params,
				(rs, number) -> row(rs, null, null));
		return new PageImpl<>(rows, PageRequest.of(page, size), total);
	}

	private MapSqlParameterSource parameters(String query, int page, int size) {
		return new MapSqlParameterSource("query", query).addValue("size", size)
				.addValue("offset", (long) page * size);
	}

	private Row row(ResultSet rs, Integer rank, Integer previousRank) throws SQLException {
		return new Row(rs.getLong("id"), rs.getLong("recipe_id"), rs.getString("title"),
				rs.getTimestamp("created_at").toLocalDateTime(), rank, previousRank);
	}
}
