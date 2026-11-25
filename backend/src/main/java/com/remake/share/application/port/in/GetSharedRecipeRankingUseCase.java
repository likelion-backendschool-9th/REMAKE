package com.remake.share.application.port.in;

import java.time.LocalDateTime;
import java.util.List;

public interface GetSharedRecipeRankingUseCase {

	WeeklyTop weeklyTop();

	Ranking ranking(String sort, String query, int page, int size);

	record WeeklyTop(List<TopItem> items) { }

	record TopItem(int rank, Long id, String title, String imageUrl, String ingredientSummary,
			int extraIngredientCount) { }

	record Ranking(String weekLabel, List<Item> content, int page, int size, long totalElements,
			boolean hasNext) { }

	/** latest의 순위·변동과 지난주 기록이 없는 변동은 null. 변동은 지난주 순위 - 이번 주 순위. */
	record Item(Long id, Integer rank, Integer rankChange, String title, String imageUrl,
			String ingredientSummary, int extraIngredientCount, LocalDateTime sharedAt) { }
}
