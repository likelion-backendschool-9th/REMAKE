package com.remake.share.application.port.out;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;

public interface LoadSharedRecipeRankingPort {

	Page<Row> popular(LocalDateTime weekStart, LocalDateTime now, String query, int page, int size);

	Page<Row> latest(String query, int page, int size);

	record Row(Long id, Long recipeId, String title, LocalDateTime sharedAt, Integer rank,
			Integer previousRank) { }
}
