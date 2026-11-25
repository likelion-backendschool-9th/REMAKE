package com.remake.ingredient.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.remake.ingredient.application.port.out.LoadIngredientLogPort;
import com.remake.ingredient.application.port.out.SaveIngredientLogPort;
import com.remake.ingredient.domain.DiscardedIngredientLog;
import com.remake.ingredient.domain.IngredientLog;

import lombok.RequiredArgsConstructor;

/** 사용·폐기 기록 Port 구현. 엔티티는 이 패키지 밖으로 나가지 않는다. */
@Component
@RequiredArgsConstructor
class IngredientLogPersistenceAdapter implements SaveIngredientLogPort, LoadIngredientLogPort {

	private final IngredientLogJpaRepository repository;

	@Override
	public IngredientLog save(IngredientLog log) {
		return repository.save(IngredientLogJpaEntity.from(log)).toDomain();
	}

	@Override
	public boolean existsByIngredientId(Long ingredientId) {
		return repository.existsByIngredientId(ingredientId);
	}

	@Override
	public List<DiscardedIngredientLog> findDiscardedBetween(Long userId, LocalDateTime from, LocalDateTime to) {
		return repository.findDiscardedBetween(userId, from, to);
	}
}
