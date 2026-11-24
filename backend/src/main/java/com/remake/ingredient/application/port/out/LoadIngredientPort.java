package com.remake.ingredient.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.remake.ingredient.domain.Ingredient;
import com.remake.ingredient.domain.StorageLocation;

/** 재료 읽기. 사용자 본인의 재료만 돌려주고, 없거나 다른 사용자의 재료면 비어 있다. */
public interface LoadIngredientPort {

	Optional<Ingredient> loadByIdAndUserId(Long ingredientId, Long userId);

	/** 사용자의 재료. 조건은 null이면 걸지 않고, 소비기한·id 오름차순이다. */
	List<Ingredient> loadByUserId(Long userId, StorageLocation storage, Long categoryId);

	/** 사용자의 모든 재료의 소비기한 */
	List<LocalDate> loadExpirationDates(Long userId);
}
