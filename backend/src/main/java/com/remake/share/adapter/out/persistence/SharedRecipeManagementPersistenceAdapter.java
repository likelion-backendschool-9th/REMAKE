package com.remake.share.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.remake.share.application.port.out.ManageSharedRecipePort;
import com.remake.share.domain.SharedRecipe;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class SharedRecipeManagementPersistenceAdapter implements ManageSharedRecipePort {
	private final JdbcTemplate jdbc;

	@Override
	public Page<SharedRecipe> loadMine(Long userId, int page, int size) {
		String from = " FROM shared_recipes p JOIN recipes r ON r.id = p.recipe_id WHERE r.user_id = ? AND p.status IN ('VISIBLE', 'HIDDEN')";
		Long count = jdbc.queryForObject("SELECT count(*)" + from, Long.class, userId);
		List<SharedRecipe> rows = jdbc.query("SELECT p.*" + from
				+ " ORDER BY p.created_at DESC, p.id DESC LIMIT ? OFFSET ?", (rs, n) -> row(rs), userId, size, (long) page * size);
		return new PageImpl<>(rows, PageRequest.of(page, size), count);
	}

	@Override
	public void lockDeletionAndSettlement() {
		// #297의 주간 확정도 대상 조회 전에 같은 키를 사용한다. 행/주 잠금보다 먼저 획득한다.
		jdbc.execute("SELECT pg_advisory_xact_lock(230, 1)");
	}

	@Override
	public Optional<SharedRecipe> lockOwned(Long userId, Long id) {
		return jdbc.query("""
				SELECT p.* FROM shared_recipes p JOIN recipes r ON r.id = p.recipe_id
				WHERE p.id = ? AND r.user_id = ? FOR UPDATE OF p
				""", (rs, n) -> row(rs), id, userId).stream().findFirst();
	}

	@Override
	public void markDeleted(Long id, LocalDateTime now) {
		jdbc.update("UPDATE shared_recipes SET status = 'DELETED', title = NULL, image_key = NULL, updated_at = ? WHERE id = ?", now, id);
	}

	private SharedRecipe row(ResultSet rs) throws SQLException {
		return new SharedRecipe(rs.getLong("id"), rs.getLong("recipe_id"), rs.getString("title"),
				rs.getString("image_key"), SharedRecipe.Status.valueOf(rs.getString("status")),
				rs.getTimestamp("created_at").toLocalDateTime());
	}
}
