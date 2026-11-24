package com.remake.ingredient.application.port.out;

import java.time.LocalDate;
import java.util.List;

import com.remake.ingredient.domain.ExpiredIngredient;
import com.remake.ingredient.domain.ExpiredIngredientCount;
import com.remake.ingredient.domain.ExpiringIngredient;

/** 알림용 재료 읽기. ingredient 기능의 Service만 쓴다. */
public interface LoadExpiryAlertIngredientPort {

	/** 소비기한이 from 이상 to 이하인 재료 */
	List<ExpiringIngredient> loadExpiringBetween(LocalDate from, LocalDate to);

	/** 소비기한이 date보다 이전인 재료 개수, 사용자별 */
	List<ExpiredIngredientCount> countExpiredBefore(LocalDate date);

	/** 소비기한이 date인 재료 */
	List<ExpiredIngredient> loadExpiredOn(LocalDate date);
}
