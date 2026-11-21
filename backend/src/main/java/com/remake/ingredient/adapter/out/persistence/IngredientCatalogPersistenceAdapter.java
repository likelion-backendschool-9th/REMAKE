package com.remake.ingredient.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.remake.ingredient.application.port.out.LoadIngredientCatalogPort;
import com.remake.ingredient.domain.IngredientCatalogItem;

import lombok.RequiredArgsConstructor;

/** 재료 사전 읽기 Port 구현. 엔티티는 이 패키지 밖으로 나가지 않는다. */
@Component
@RequiredArgsConstructor
class IngredientCatalogPersistenceAdapter implements LoadIngredientCatalogPort {

	private final IngredientCatalogJpaRepository repository;

	@Override
	public List<IngredientCatalogItem> searchByName(String keyword) {
		return repository.searchByName(LikePattern.contains(keyword)).stream()
				.map(IngredientCatalogJpaEntity::toDomain)
				.toList();
	}

	@Override
	public List<IngredientCatalogItem> loadByIds(Collection<Long> ids) {
		return repository.findAllById(ids).stream()
				.map(IngredientCatalogJpaEntity::toDomain)
				.toList();
	}

	@Override
	public Optional<IngredientCatalogItem> loadByName(String name) {
		return repository.findByName(name).map(IngredientCatalogJpaEntity::toDomain);
	}
}
