package com.remake.ingredient.application.port.in;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.remake.ingredient.domain.IngredientCatalogEntry;
import com.remake.ingredient.domain.IngredientCatalogItem;

/**
 * 재료 사전 조회. 자동완성 API와 다른 기능(온보딩의 제외 재료, 영수증 품목 정규화, AI 요청)이 같은 기준 데이터를 쓰는 입구이다.
 * 다른 기능은 이 UseCase만 부르고 사전 테이블을 직접 읽지 않는다.
 */
public interface GetIngredientCatalogUseCase {

	/**
	 * 이름 자동완성·제외 재료 검색. keyword는 선택이고(없으면 전체) 50자 이하여야 한다(아니면 INVALID_INPUT).
	 * 앞부분이 맞는 이름이 먼저, 그다음 이름순이고, commonExclusionFirst면 대표 알레르기 재료를 맨 위에 둔다. 최대 50개.
	 * 결과에는 카테고리 code와 보관 일수(항목 값이 없으면 카테고리 기본값)가 붙어 있다.
	 */
	List<IngredientCatalogEntry> searchCatalog(String keyword, boolean commonExclusionFirst);

	/** id로 읽는다. 없는 id는 결과에 없으니 호출한 쪽이 개수를 비교해 CATALOG_ITEM_NOT_FOUND 같은 오류로 바꾼다. */
	List<IngredientCatalogItem> getCatalogItemsByIds(Collection<Long> ids);

	/** 이름이 정확히 같은 항목(앞뒤 공백 무시). 영수증 품목을 사전에 맞출 때 쓴다. */
	Optional<IngredientCatalogItem> findCatalogItemByName(String name);
}
