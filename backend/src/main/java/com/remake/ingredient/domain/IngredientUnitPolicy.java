package com.remake.ingredient.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 재료명으로 단위를 정하는 규칙(#145 결정). 순서는 다음과 같다.
 * <ol>
 * <li>재료명이 사전 이름과 정확히 같으면 그 단위</li>
 * <li>없으면 재료명 안에 들어 있는 사전 이름 중 카테고리가 같은 것의 단위. 두 글자 이상인 사전 이름만 보고, 여러 개면 가장 긴 이름을 쓴다</li>
 * <li>그래도 없으면 {@link #DEFAULT_UNIT}</li>
 * </ol>
 * 이 클래스는 2번에서 후보 가운데 고르는 일만 한다. 사전을 읽는 일은 Service가 한다.
 */
public final class IngredientUnitPolicy {

	/** 사전에서 정하지 못했을 때 쓰는 최종 단위 */
	public static final IngredientUnit DEFAULT_UNIT = IngredientUnit.EA;

	/** 재료명 안에서 찾을 사전 이름의 최소 글자 수. 한 글자 이름(무, 배, 김)은 정확히 같을 때만 인정한다 */
	static final int MIN_CONTAINED_NAME_LENGTH = 2;

	private IngredientUnitPolicy() {
	}

	/**
	 * 재료명 안에 들어 있는 사전 항목 가운데 가장 긴 이름을 돌려준다. 길이가 같으면 id가 작은 것을 쓴다.
	 * 후보는 호출한 쪽이 같은 카테고리 항목만 넘긴다. 비교는 이름 그대로(대소문자 구분)이고 사전 정확 조회와 같다.
	 */
	public static Optional<IngredientCatalogItem> findContainedItem(String name,
			List<IngredientCatalogItem> sameCategoryItems) {
		if (name == null || sameCategoryItems == null) {
			return Optional.empty();
		}
		return sameCategoryItems.stream()
				.filter(item -> item.name().length() >= MIN_CONTAINED_NAME_LENGTH)
				.filter(item -> name.contains(item.name()))
				.max(Comparator.comparingInt((IngredientCatalogItem item) -> item.name().length())
						.thenComparing(IngredientCatalogItem::id, Comparator.reverseOrder()));
	}
}
