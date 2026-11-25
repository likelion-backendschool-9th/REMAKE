package com.remake.share.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.recipe.application.port.in.GetRecipeForSharingUseCase;
import com.remake.recipe.domain.IngredientSummary;
import com.remake.share.application.port.in.GetSharedRecipeRankingUseCase;
import com.remake.share.application.port.out.LoadSharedRecipeRankingPort;
import com.remake.share.application.port.out.LoadSharedRecipeRankingPort.Row;
import com.remake.share.domain.SharedRecipe;
import com.remake.share.domain.SharedRecipeRankingPolicy;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class SharedRecipeRankingService implements GetSharedRecipeRankingUseCase {
	private final LoadSharedRecipeRankingPort rankings;
	private final GetRecipeForSharingUseCase recipes;
	private final Clock clock;

	@Override
	public WeeklyTop weeklyTop() {
		var rows = rankings.popular(SharedRecipe.weekStart(clock), now(), "", 0,
				SharedRecipeRankingPolicy.TOP_LIMIT);
		return new WeeklyTop(rows.getContent().stream().map(row -> {
			var summary = summary(row);
			return new TopItem(row.rank(), row.id(), row.title(), imageUrl(row), summary.summary(),
					summary.extraCount());
		}).toList());
	}

	@Override
	public Ranking ranking(String sort, String query, int page, int size) {
		if ((!"popular".equals(sort) && !"latest".equals(sort)) || page < 0 || size < 1
				|| size > SharedRecipeRankingPolicy.MAX_PAGE_SIZE) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		String search = query == null ? "" : query.strip();
		LocalDateTime weekStart = SharedRecipe.weekStart(clock);
		var rows = "popular".equals(sort) ? rankings.popular(weekStart, now(), search, page, size)
				: rankings.latest(search, page, size);
		return new Ranking(SharedRecipeRankingPolicy.weekLabel(weekStart.toLocalDate()),
				rows.getContent().stream().map(this::item).toList(), page, size, rows.getTotalElements(),
				rows.hasNext());
	}

	private Item item(Row row) {
		var summary = summary(row);
		return new Item(row.id(), row.rank(), SharedRecipeRankingPolicy.rankChange(row.rank(), row.previousRank()),
				row.title(), imageUrl(row), summary.summary(), summary.extraCount(), row.sharedAt());
	}

	private IngredientSummary summary(Row row) {
		return IngredientSummary.of(recipes.getSharedContent(row.recipeId()).ingredients().stream()
				.map(ingredient -> ingredient.name()).toList());
	}

	private String imageUrl(Row row) {
		return "/api/v1/user/shared-recipe-photos/" + row.id();
	}

	private LocalDateTime now() {
		return LocalDateTime.now(clock.withZone(ZoneId.of("Asia/Seoul")));
	}
}
