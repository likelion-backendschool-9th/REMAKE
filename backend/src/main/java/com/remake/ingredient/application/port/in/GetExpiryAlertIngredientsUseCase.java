package com.remake.ingredient.application.port.in;

import java.util.List;

import com.remake.ingredient.domain.ExpiredIngredient;
import com.remake.ingredient.domain.ExpiredIngredientCount;
import com.remake.ingredient.domain.ExpiringIngredient;

/**
 * 소비기한 알림 배치가 쓰는 재료 조회. 알림 설정(ON/OFF)과 중복 확인은 알림 쪽에서 하고, 여기서는 재료만 판정한다.
 * 기준일은 주입받은 Clock의 오늘이다. 기준은 {@link com.remake.ingredient.domain.ExpiryPolicy}.
 */
public interface GetExpiryAlertIngredientsUseCase {

	/** 임박 재료: 오늘~3일 뒤(오늘이 기한인 재료 포함). 사용자, 소비기한, 재료 번호 순 */
	List<ExpiringIngredient> getExpiringIngredients();

	/** 만료 재료 개수: 소비기한이 오늘보다 이전. 만료 재료가 있는 사용자만, 사용자 번호 순 */
	List<ExpiredIngredientCount> getExpiredIngredientCounts();

	/** 어제 만료된 재료(ingredient_expired 이벤트용). 사용자, 재료 번호 순 */
	List<ExpiredIngredient> getIngredientsExpiredYesterday();
}
