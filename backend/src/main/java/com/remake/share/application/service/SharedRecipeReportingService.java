package com.remake.share.application.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.share.application.port.in.ReportSharedRecipeUseCase;
import com.remake.share.application.port.out.LoadSharedRecipePort;
import com.remake.share.application.port.out.SaveSharedRecipeReportPort;
import com.remake.share.domain.SharedRecipe;
import com.remake.share.domain.SharedRecipeReportReason;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class SharedRecipeReportingService implements ReportSharedRecipeUseCase {
	private final LoadSharedRecipePort sharedRecipes;
	private final SaveSharedRecipeReportPort reports;
	private final EventTracker events;

	@Override
	@Transactional
	public void report(Long userId, Long sharedRecipeId, SharedRecipeReportReason reason) {
		if (reason == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		sharedRecipes.findById(sharedRecipeId).filter(p -> p.status() == SharedRecipe.Status.VISIBLE)
				.orElseThrow(() -> new BusinessException(ErrorCode.SHARED_RECIPE_NOT_FOUND));
		reports.save(userId, sharedRecipeId, reason);
		events.track(EventName.SHARED_RECIPE_REPORTED, Screen.SHARED_REPORT,
				Map.of("shared_recipe_id", sharedRecipeId, "report_reason", reason.eventValue()),
				EventTrackCommand.ofUser(userId));
	}
}
