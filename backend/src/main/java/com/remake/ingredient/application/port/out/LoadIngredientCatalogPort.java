package com.remake.ingredient.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.remake.ingredient.domain.IngredientCatalogItem;

/** 재료 사전 읽기. 기준 데이터라 읽기만 한다. ingredient 기능의 Service만 쓴다. */
public interface LoadIngredientCatalogPort {

	List<IngredientCatalogItem> loadByIds(Collection<Long> ids);

	Optional<IngredientCatalogItem> loadByName(String name);
}
