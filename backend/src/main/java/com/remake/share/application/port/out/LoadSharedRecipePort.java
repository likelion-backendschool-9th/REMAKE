package com.remake.share.application.port.out;

import java.time.LocalDateTime;
import java.util.Optional;
import com.remake.share.domain.SharedRecipe;

public interface LoadSharedRecipePort {
	boolean existsInWeek(Long recipeId, LocalDateTime start, LocalDateTime end);
	Optional<SharedRecipe> findById(Long id);
}
