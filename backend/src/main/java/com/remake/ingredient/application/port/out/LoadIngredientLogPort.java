package com.remake.ingredient.application.port.out;

import java.time.LocalDateTime;
import java.util.List;

import com.remake.ingredient.domain.DiscardedIngredientLog;

/** 사용·폐기 기록 읽기. */
public interface LoadIngredientLogPort {

	/** 이 재료에 사용·폐기 기록이 하나라도 있는지 */
	boolean existsByIngredientId(Long ingredientId);

	/** 회원이 [from, to)에 버린 기록. 최근에 버린 순(같은 시각이면 기록 번호 큰 순) */
	List<DiscardedIngredientLog> findDiscardedBetween(Long userId, LocalDateTime from, LocalDateTime to);
}
