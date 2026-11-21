package com.remake.ingredient.application.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.GetIngredientCatalogUseCase;
import com.remake.ingredient.application.port.out.LoadIngredientCatalogPort;
import com.remake.ingredient.domain.IngredientCatalogItem;

import lombok.RequiredArgsConstructor;

/** 재료 사전 조회. 검색 결과의 정렬과 개수 제한은 여기서 정한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientCatalogService implements GetIngredientCatalogUseCase {

	static final int MAX_KEYWORD_LENGTH = 50;
	static final int MAX_RESULTS = 50;

	private final LoadIngredientCatalogPort loadIngredientCatalogPort;

	@Override
	public List<IngredientCatalogItem> searchCatalog(String keyword, boolean commonExclusionFirst) {
		String trimmed = keyword == null ? "" : keyword.strip();
		if (trimmed.length() > MAX_KEYWORD_LENGTH) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "검색어는 50자 이하여야 합니다.");
		}
		String lower = trimmed.toLowerCase(Locale.ROOT);
		// 대표 알레르기 재료(맨 위, 선택) → 이름이 검색어로 시작 → 이름순
		Comparator<IngredientCatalogItem> order = Comparator
				.comparing((IngredientCatalogItem item) -> !(commonExclusionFirst && item.commonExclusion()))
				.thenComparing(item -> !item.name().toLowerCase(Locale.ROOT).startsWith(lower))
				.thenComparing(IngredientCatalogItem::name);
		return loadIngredientCatalogPort.searchByName(trimmed).stream()
				.sorted(order)
				.limit(MAX_RESULTS)
				.toList();
	}

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
