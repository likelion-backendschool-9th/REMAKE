package com.remake.ingredient.application.port.in;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.remake.ingredient.domain.IngredientCatalogItem;

/**
 * 재료 사전 조회. 사전은 서버 안에서만 쓰는 기준 데이터이고(11/22 결정, 공개 API 없음), 재료명으로 단위·카테고리·보관 일수를
 * 정할 때 쓴다(재료 추가·수정, 영수증 등록). 다른 기능은 이 UseCase만 부르고 사전 테이블을 직접 읽지 않는다.
 */
public interface GetIngredientCatalogUseCase {

	/** id로 읽는다. 없는 id는 결과에 없으니 호출한 쪽이 개수를 비교해 알맞은 오류로 바꾼다. */
	List<IngredientCatalogItem> getCatalogItemsByIds(Collection<Long> ids);

	/** 이름이 정확히 같은 항목(앞뒤 공백 무시). 사전에 없으면 빈 값이다. 단위를 정하려면 이 메서드가 아니라 ResolveIngredientUnitUseCase를 쓴다. */
	Optional<IngredientCatalogItem> findCatalogItemByName(String name);
}
