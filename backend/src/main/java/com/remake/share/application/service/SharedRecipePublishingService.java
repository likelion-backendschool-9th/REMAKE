package com.remake.share.application.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.recipe.application.port.in.GetRecipeForSharingUseCase;
import com.remake.share.application.port.in.PublishSharedRecipeUseCase;
import com.remake.share.application.port.in.SharedRecipePhotoUpload;
import com.remake.share.application.port.in.StoreSharedRecipePhotoUseCase;
import com.remake.share.application.port.out.LoadSharedRecipePort;
import com.remake.share.application.port.out.SaveSharedRecipePort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class SharedRecipePublishingService implements PublishSharedRecipeUseCase {
	private final GetRecipeForSharingUseCase recipes;
	private final LoadSharedRecipePort sharedRecipes;
	private final SaveSharedRecipePort saveSharedRecipe;
	private final StoreSharedRecipePhotoUseCase photos;
	private final EventTracker events;
	private final Clock clock;

	@Override
	@Transactional
	public Long publish(Long userId, Long recipeId, String title, SharedRecipePhotoUpload photo) {
		if (recipeId == null || recipeId <= 0 || title == null || title.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		recipes.getForPublishing(userId, recipeId);
		LocalDateTime start = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")))
				.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
		if (sharedRecipes.existsInWeek(recipeId, start, start.plusWeeks(1))) {
			throw new BusinessException(ErrorCode.ALREADY_SHARED);
		}
		String key = photos.store(photo);
		Long id = saveSharedRecipe.save(recipeId, title.strip(), key);
		events.track(EventName.SHARED_RECIPE_PUBLISHED, Screen.SHARED_WRITE,
				Map.of("shared_recipe_id", id), EventTrackCommand.ofUser(userId));
		return id;
	}
}
