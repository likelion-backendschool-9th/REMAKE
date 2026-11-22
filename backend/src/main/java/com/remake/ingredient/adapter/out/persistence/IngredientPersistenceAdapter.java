package com.remake.ingredient.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.out.DeleteIngredientPort;
import com.remake.ingredient.application.port.out.LoadIngredientPort;
import com.remake.ingredient.application.port.out.SaveIngredientPort;
import com.remake.ingredient.domain.Ingredient;
import com.remake.ingredient.domain.IngredientStatus;
import com.remake.ingredient.domain.StorageLocation;

import lombok.RequiredArgsConstructor;

/** 재료 읽기·저장·삭제 Port 구현. 엔티티를 도메인 객체로 바꿔 주고받고, 엔티티는 이 패키지 밖으로 나가지 않는다. */
@Component
@RequiredArgsConstructor
class IngredientPersistenceAdapter implements LoadIngredientPort, SaveIngredientPort, DeleteIngredientPort {

	private final IngredientJpaRepository repository;

	@Override
	public Optional<Ingredient> loadByIdAndUserId(Long ingredientId, Long userId) {
		return repository.findByIdAndUserId(ingredientId, userId).map(IngredientJpaEntity::toDomain);
	}

	@Override
	public List<Ingredient> loadActiveByUserId(Long userId, StorageLocation storage, Long categoryId) {
		return repository.search(userId, IngredientStatus.ACTIVE, storage, categoryId).stream()
				.map(IngredientJpaEntity::toDomain)
				.toList();
	}

	@Override
	public List<LocalDate> loadActiveExpirationDates(Long userId) {
		return repository.findExpirationDates(userId, IngredientStatus.ACTIVE);
	}

	@Override
	public Ingredient save(Ingredient ingredient) {
		if (ingredient.id() == null) {
			return repository.save(IngredientJpaEntity.from(ingredient)).toDomain();
		}
		// 이미 있는 재료는 불러와서 값을 옮긴다. 트랜잭션이 끝날 때 변경 감지로 저장되고 @Version이 충돌을 막는다.
		IngredientJpaEntity entity = repository.findById(ingredient.id())
				.orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));
		entity.apply(ingredient);
		return entity.toDomain();
	}

	@Override
	public void deleteById(Long ingredientId) {
		repository.deleteById(ingredientId);
	}
}
