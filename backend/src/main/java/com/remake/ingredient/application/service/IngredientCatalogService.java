package com.remake.ingredient.application.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.ingredient.application.port.in.GetIngredientCatalogUseCase;
import com.remake.ingredient.application.port.out.LoadIngredientCatalogPort;
import com.remake.ingredient.domain.IngredientCatalogItem;

import lombok.RequiredArgsConstructor;

/** 재료 사전 조회. 서버 안에서만 쓴다(공개 API 없음). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientCatalogService implements GetIngredientCatalogUseCase {

	private final LoadIngredientCatalogPort loadIngredientCatalogPort;

	@Override
	public List<IngredientCatalogItem> getCatalogItemsByIds(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		return loadIngredientCatalogPort.loadByIds(ids);
	}

	@Override
	public Optional<IngredientCatalogItem> findCatalogItemByName(String name) {
		if (name == null || name.isBlank()) {
			return Optional.empty();
		}
		return loadIngredientCatalogPort.loadByName(name.strip());
	}
}
