package com.remake.share.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.share.application.port.in.CheckRecipeSharedThisWeekUseCase;
import com.remake.share.application.port.out.LoadSharedRecipePort;
import com.remake.share.domain.SharedRecipe;

import lombok.RequiredArgsConstructor;

/** 게시(SharedRecipePublishingService)의 ALREADY_SHARED 검사와 같은 기준. 숨긴 글도 이번 주 공유로 센다. */
@Service
@RequiredArgsConstructor
class SharedRecipeWeekQueryService implements CheckRecipeSharedThisWeekUseCase {

	private final LoadSharedRecipePort sharedRecipes;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public boolean isSharedThisWeek(Long recipeId) {
		LocalDateTime start = SharedRecipe.weekStart(clock);
		return sharedRecipes.existsInWeek(recipeId, start, start.plusWeeks(1));
	}
}
