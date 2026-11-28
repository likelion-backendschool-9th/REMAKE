package com.remake.share.application.port.in;

import java.time.LocalDateTime;

import com.remake.global.common.PageResponse;
import com.remake.share.domain.SharedRecipe;
import io.swagger.v3.oas.annotations.media.Schema;

public interface GetMySharedRecipesUseCase {
	PageResponse<Item> getMine(Long userId, int page, int size);

	@Schema(name = "MySharedRecipeItem")
	record Item(Long id, String title, @Schema(nullable = true) String imageUrl,
			SharedRecipe.Status status, LocalDateTime sharedAt, boolean canDelete) { }
}
