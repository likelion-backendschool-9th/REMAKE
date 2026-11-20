package com.remake.ingredient.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.remake.ingredient.application.port.out.LoadIngredientCategoryPort;
import com.remake.ingredient.domain.IngredientCategory;
import com.remake.ingredient.domain.IngredientCategoryCode;

import lombok.RequiredArgsConstructor;

/** Port 구현. JPA 엔티티를 도메인 객체로 바꿔서 돌려준다. 엔티티는 이 패키지 밖으로 나가지 않는다. */
@Component
@RequiredArgsConstructor
class IngredientCategoryPersistenceAdapter implements LoadIngredientCategoryPort {

	private final IngredientCategoryJpaRepository repository;

	@Override
	public List<IngredientCategory> loadAll() {
		return repository.findAllByOrderByIdAsc().stream()
				.map(IngredientCategoryJpaEntity::toDomain)
				.toList();
	}

	@Override
	public Optional<IngredientCategory> loadById(Long categoryId) {
		return repository.findById(categoryId).map(IngredientCategoryJpaEntity::toDomain);
	}

	@Override
	public Optional<IngredientCategory> loadByCode(IngredientCategoryCode code) {
		return repository.findByCode(code).map(IngredientCategoryJpaEntity::toDomain);
	}
}
